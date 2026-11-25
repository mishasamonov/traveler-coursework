package ua.edu.sumdu.traveler.web;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Layers of the whole application: presentation (web) -> business logic (core) <- data access
 * (persistence). Controllers never touch JPA entities or Spring Data repositories.
 */
@AnalyzeClasses(packages = "ua.edu.sumdu.traveler", importOptions = ImportOption.DoNotIncludeTests.class)
class WebArchitectureTest {

  @ArchTest
  static final ArchRule layers = layeredArchitecture().consideringOnlyDependenciesInLayers()
      .layer("Presentation").definedBy("ua.edu.sumdu.traveler.web..")
      .layer("Business").definedBy("ua.edu.sumdu.traveler.core..")
      .layer("DataAccess").definedBy("ua.edu.sumdu.traveler.persistence..")
      .whereLayer("Presentation").mayNotBeAccessedByAnyLayer()
      .whereLayer("DataAccess").mayOnlyBeAccessedByLayers("Presentation")
      .whereLayer("Business").mayOnlyBeAccessedByLayers("Presentation", "DataAccess");

  /** The web module only imports PersistenceConfig; it never uses entities or repositories. */
  @ArchTest
  static final ArchRule controllersUseServicesOnly = noClasses()
      .that().resideInAPackage("ua.edu.sumdu.traveler.web..")
      .should().dependOnClassesThat().resideInAnyPackage(
          "ua.edu.sumdu.traveler.persistence.entity..",
          "ua.edu.sumdu.traveler.persistence.repository..",
          "ua.edu.sumdu.traveler.persistence.adapter..");
}
