package dev.yeetle.classicskyblock.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import dev.yeetle.classicskyblock.common.persistence.SaveData;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * The architecture of the project, which consists of three parts:
 * <ul>
 * <li>the technical layers (api, domain, persistence and value objects) in every domain package,
 * <li>the functional layers (common, player, islandmanagement and locations),
 * <li>the pattern language, which gives every class a role.
 * </ul>
 * The entry point is the only class that is not part of these parts.
 * It wires everything together and may therefore access everything.
 */
@AnalyzeClasses(packages = ArchitectureTest.ROOT, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
	static final String ROOT = "dev.yeetle.classicskyblock";

	private static final String COMMON = ROOT + ".common..";
	private static final String PLAYER = ROOT + ".player..";
	private static final String ISLAND_MANAGEMENT = ROOT + ".islandmanagement..";
	private static final String LOCATIONS = ROOT + ".locations..";

	private static final String API = "api";
	private static final String DOMAIN = "domain";
	private static final String PERSISTENCE = "persistence";
	private static final String VALUE_OBJECTS = "valueobjects";

	private static final java.util.regex.Pattern PERSISTENCE_PACKAGE =
		java.util.regex.Pattern.compile(java.util.regex.Pattern.quote(ROOT) + "\\.([^.]+)\\.persistence(\\..*)?");
	private static final java.util.regex.Pattern DOMAIN_PACKAGE =
		java.util.regex.Pattern.compile(java.util.regex.Pattern.quote(ROOT) + "\\.([^.]+)\\.domain(\\..*)?");

	private static final DescribedPredicate<JavaClass> ENTRY_POINT = JavaClass.Predicates.simpleName("SkyblockPlugin")
		.and(resideInAPackage(ROOT))
		.as("the entry point");

	/**
	 * Matches the packages of a technical layer in every domain package, for example <code>..player.api..</code>.
	 */
	private static String layerOfAnyDomain(String layer) {
		return ROOT + ".*." + layer + "..";
	}

	// ---------------------------------------------------------------------------------------------------------------
	// Technical layers
	// The api triggers the code, the domain contains the business logic and the persistence saves the data.
	// ---------------------------------------------------------------------------------------------------------------

	@ArchTest
	static final ArchRule technicalLayersAreRespected = layeredArchitecture()
		.consideringOnlyDependenciesInLayers()
		.layer("Api").definedBy(layerOfAnyDomain(API))
		.layer("Domain").definedBy(layerOfAnyDomain(DOMAIN))
		.layer("Persistence").definedBy(layerOfAnyDomain(PERSISTENCE))
		.layer("ValueObjects").definedBy(layerOfAnyDomain(VALUE_OBJECTS))
		// Nothing calls the api, because it is what triggers our code.
		.whereLayer("Api").mayNotBeAccessedByAnyLayer()
		// The domain is called by the api and by other domains.
		.whereLayer("Domain").mayOnlyBeAccessedByLayers("Api")
		// The persistence is only called by the domain, the api has no direct access to it.
		.whereLayer("Persistence").mayOnlyBeAccessedByLayers("Domain")
		// Value objects are used by every layer, but know none of them, because no layer may be accessed by them.
		.whereLayer("ValueObjects").mayOnlyBeAccessedByLayers("Api", "Domain", "Persistence");

	@ArchTest
	static final ArchRule domainPackagesOnlyContainTheTechnicalLayers = classes()
		.that().resideInAnyPackage(ROOT + ".*..")
		.and().resideOutsideOfPackage(ROOT)
		.should().resideInAnyPackage(
			layerOfAnyDomain(API),
			layerOfAnyDomain(DOMAIN),
			layerOfAnyDomain(PERSISTENCE),
			layerOfAnyDomain(VALUE_OBJECTS)
		)
		.as("domain packages should only contain the packages api, domain, persistence and valueobjects");

	/**
	 * The save data of the common package is shared by all domains and therefore does not count as foreign persistence.
	 * It can be seen by every domain until the data is migrated to one file per domain.
	 */
	@ArchTest
	static final ArchRule domainOnlyAccessesItsOwnPersistence = noClasses()
		.that().resideInAPackage(layerOfAnyDomain(DOMAIN))
		.should(accessPersistenceOfAnotherDomain())
		.as("the domain should only access the persistence of its own domain, other persistence is reached through the service of that domain");

	// ---------------------------------------------------------------------------------------------------------------
	// Functional layers
	// Every layer may only use the layers below it.
	// ---------------------------------------------------------------------------------------------------------------

	@ArchTest
	static final ArchRule functionalLayersAreRespected = layeredArchitecture()
		.consideringOnlyDependenciesInLayers()
		.layer("Common").definedBy(COMMON)
		.layer("Player").definedBy(PLAYER)
		.layer("IslandManagement").definedBy(ISLAND_MANAGEMENT)
		.layer("Locations").definedBy(LOCATIONS)
		// Every layer is only used by the layers above it, so common is used by everyone but uses nothing.
		.whereLayer("Common").mayOnlyBeAccessedByLayers("Player", "IslandManagement", "Locations")
		.whereLayer("Player").mayOnlyBeAccessedByLayers("IslandManagement", "Locations")
		.whereLayer("IslandManagement").mayOnlyBeAccessedByLayers("Locations")
		.whereLayer("Locations").mayNotBeAccessedByAnyLayer();

	@ArchTest
	static final ArchRule everyClassBelongsToAFunctionalLayerOrIsTheEntryPoint = classes()
		.that(not(ENTRY_POINT))
		.should().resideInAnyPackage(COMMON, PLAYER, ISLAND_MANAGEMENT, LOCATIONS)
		.as("every class should belong to a functional layer or be the entry point");

	@ArchTest
	static final ArchRule onlyTheEntryPointResidesInTheRootPackage = classes()
		.that().resideInAPackage(ROOT)
		.should().haveSimpleName("SkyblockPlugin")
		.as("only the entry point should reside directly in the root package");

	@ArchTest
	static final ArchRule nobodyUsesTheEntryPoint = noClasses()
		.that(not(ENTRY_POINT))
		.should().dependOnClassesThat(ENTRY_POINT);

	/**
	 * All data is still saved in one file, so the repositories share the {@link SaveData}.
	 * Every domain may only touch the part that will be moved to its own file.
	 */
	@ArchTest
	static final ArchRule onlyThePlayerDomainTouchesThePlayersOfTheSaveData = noClasses()
		.that().resideOutsideOfPackages(PLAYER)
		.should().callMethod(SaveData.class, "getPlayers")
		.as("only the player domain should touch the players of the save data");

	@ArchTest
	static final ArchRule onlyTheIslandManagementTouchesTheIslandsOfTheSaveData = noClasses()
		.that().resideOutsideOfPackages(ISLAND_MANAGEMENT)
		.should().callMethod(SaveData.class, "getIslands")
		.as("only the island management should touch the islands of the save data");

	// ---------------------------------------------------------------------------------------------------------------
	// Pattern language
	// Every class has a role, which is recognized by its name and its technical layer.
	// The roles are defined in PATTERNS, the rules define what each role has to look like and who may use it.
	// ---------------------------------------------------------------------------------------------------------------

	/**
	 * A role of a class.
	 *
	 * @param name   the name of the pattern
	 * @param layer  the technical layer of the pattern
	 * @param suffix the end of the class name or null if the pattern has no naming convention
	 */
	private record Pattern(String name, String layer, String suffix) {
		DescribedPredicate<JavaClass> matches() {
			DescribedPredicate<JavaClass> inLayer = resideInAPackage(layerOfAnyDomain(layer));

			return suffix == null
				? inLayer.as(name)
				: inLayer.and(simpleNameEndingWith(suffix)).as(name);
		}

		DescribedPredicate<JavaClass> named() {
			return simpleNameEndingWith(suffix).as("named *" + suffix);
		}
	}

	/**
	 * Implements a Bukkit listener that reacts to events.
	 */
	private static final Pattern EVENT_HANDLER = new Pattern("EventHandler", API, "EventHandler");
	/**
	 * Provides one or more commands.
	 */
	private static final Pattern COMMAND_HANDLER = new Pattern("CommandHandler", API, "CommandHandler");
	/**
	 * Implements the business logic.
	 */
	private static final Pattern SERVICE = new Pattern("Service", DOMAIN, "Service");
	/**
	 * Builds a part of the world or generates it.
	 */
	private static final Pattern GENERATOR = new Pattern("Generator", DOMAIN, "Generator");
	/**
	 * The interface to the persistence.
	 */
	private static final Pattern REPOSITORY = new Pattern("Repository", PERSISTENCE, "Repository");
	/**
	 * Reads and writes the data of exactly one file.
	 */
	private static final Pattern FILE_MANAGER = new Pattern("FileManager", PERSISTENCE, "FileManager");
	/**
	 * The content of a file, which is read and written by a file manager.
	 */
	private static final Pattern DATA = new Pattern("Data", PERSISTENCE, "Data");
	/**
	 * An immutable value without an identity, which may be used in every layer.
	 */
	private static final Pattern VALUE_OBJECT = new Pattern("ValueObject", VALUE_OBJECTS, null);

	private static final List<Pattern> PATTERNS = List.of(
		EVENT_HANDLER, COMMAND_HANDLER, SERVICE, GENERATOR, REPOSITORY, FILE_MANAGER, DATA, VALUE_OBJECT
	);

	@ArchTest
	static final ArchRule everyClassHasExactlyOnePattern = classes()
		.that().areTopLevelClasses()
		.and(not(ENTRY_POINT))
		.should(matchExactlyOnePattern())
		.as("every class should match exactly one pattern of the pattern language, which is "
			+ PATTERNS.stream().map(Pattern::name).collect(Collectors.joining(", "))
			+ " (the entry point is the only exception)");

	@ArchTest
	static final ArchRule eventHandlersImplementAListener = classes()
		.that(EVENT_HANDLER.named())
		.should().implement(Listener.class);

	@ArchTest
	static final ArchRule listenersAreEventHandlers = classes()
		.that().implement(Listener.class)
		.and(not(ENTRY_POINT))
		.should(ArchCondition.from(EVENT_HANDLER.matches()));

	@ArchTest
	static final ArchRule commandsAreBuiltByCommandHandlers = classes()
		.that(DescribedPredicate.describe("use the brigadier commands of Paper", clazz -> clazz.getDirectDependenciesFromSelf().stream()
			.anyMatch(dependency -> dependency.getTargetClass().getPackageName().startsWith("io.papermc.paper.command.brigadier"))))
		.and(not(ENTRY_POINT))
		.should(ArchCondition.from(COMMAND_HANDLER.matches()));

	@ArchTest
	static final ArchRule namedLikeAPatternMeansLivingInItsLayer = classes()
		.that().areTopLevelClasses()
		.and(not(ENTRY_POINT))
		.and(DescribedPredicate.describe("named like a pattern", clazz -> PATTERNS.stream()
			.filter(pattern -> pattern.suffix() != null)
			.anyMatch(pattern -> pattern.named().test(clazz))))
		.should(matchExactlyOnePattern())
		.as("a class that is named like a pattern should also be in the technical layer of that pattern");

	@ArchTest
	static final ArchRule generatorsAreOnlyUsedByServices = classes()
		.that(GENERATOR.matches())
		.should().onlyHaveDependentClassesThat(SERVICE.matches().or(GENERATOR.matches()).or(ENTRY_POINT))
		.as("generators should only be used by services and other generators");

	@ArchTest
	static final ArchRule repositoriesAreOnlyUsedByServicesAndRepositories = classes()
		.that(REPOSITORY.matches())
		.should().onlyHaveDependentClassesThat(SERVICE.matches().or(REPOSITORY.matches()).or(ENTRY_POINT))
		.as("repositories should only be used by services and other repositories");

	@ArchTest
	static final ArchRule fileManagersAreOnlyUsedByRepositories = classes()
		.that(FILE_MANAGER.matches())
		.should().onlyHaveDependentClassesThat(REPOSITORY.matches().or(FILE_MANAGER.matches()).or(ENTRY_POINT))
		.as("file managers should only be used by repositories");

	// ---------------------------------------------------------------------------------------------------------------
	// Conditions
	// ---------------------------------------------------------------------------------------------------------------

	private static ArchCondition<JavaClass> accessPersistenceOfAnotherDomain() {
		return new ArchCondition<>("access the persistence of another domain") {
			@Override
			public void check(JavaClass origin, ConditionEvents events) {
				Matcher originMatcher = DOMAIN_PACKAGE.matcher(origin.getPackageName());

				if (!originMatcher.matches()) {
					return;
				}

				for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
					Matcher targetMatcher = PERSISTENCE_PACKAGE.matcher(dependency.getTargetClass().getPackageName());

					if (targetMatcher.matches()
						&& !targetMatcher.group(1).equals("common")
						&& !targetMatcher.group(1).equals(originMatcher.group(1))) {
						events.add(SimpleConditionEvent.satisfied(origin, dependency.getDescription()));
					}
				}
			}
		};
	}

	private static ArchCondition<JavaClass> matchExactlyOnePattern() {
		return new ArchCondition<>("match exactly one pattern") {
			@Override
			public void check(JavaClass clazz, ConditionEvents events) {
				List<String> matching = PATTERNS.stream()
					.filter(pattern -> pattern.matches().test(clazz))
					.map(Pattern::name)
					.toList();

				if (matching.size() != 1) {
					events.add(SimpleConditionEvent.violated(clazz,
						"%s matches %s patterns %s".formatted(clazz.getName(), matching.size(), matching)));
				}
			}
		};
	}
}
