package ua.edu.sumdu.traveler.persistence;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import ua.edu.sumdu.traveler.core.port.LocationPort;
import ua.edu.sumdu.traveler.core.port.TravelPlanPort;
import ua.edu.sumdu.traveler.core.port.UserPort;

/** The data access layer knows nothing about the web and implements the ports of the core. */
@AnalyzeClasses(packages = "ua.edu.sumdu.traveler.persistence",
    importOptions = ImportOption.DoNotIncludeTests.class)
class PersistenceArchitectureTest {

  @ArchTest
  static final ArchRule noWeb = noClasses().should().dependOnClassesThat()
      .resideInAnyPackage("ua.edu.sumdu.traveler.web..", "org.springframework.web..",
          "jakarta.servlet..");

  @ArchTest
  static final ArchRule adaptersImplementPorts = classes().that().haveSimpleNameStartingWith("Jpa")
      .and().haveSimpleNameEndingWith("Adapter")
      .should().implement(TravelPlanPort.class).orShould().implement(LocationPort.class)
      .orShould().implement(UserPort.class);
}
