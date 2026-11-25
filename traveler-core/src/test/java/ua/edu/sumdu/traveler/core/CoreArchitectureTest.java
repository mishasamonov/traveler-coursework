package ua.edu.sumdu.traveler.core;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** The business layer does not depend on the data access and presentation technologies. */
@AnalyzeClasses(packages = "ua.edu.sumdu.traveler.core", importOptions = ImportOption.DoNotIncludeTests.class)
class CoreArchitectureTest {

  @ArchTest
  static final ArchRule noJpaHttpOrSecurity = noClasses().should().dependOnClassesThat()
      .resideInAnyPackage("jakarta.persistence..", "org.springframework.data..",
          "org.springframework.web..", "jakarta.servlet..", "org.springframework.security..",
          "ua.edu.sumdu.traveler.persistence..", "ua.edu.sumdu.traveler.web..");

  @ArchTest
  static final ArchRule domainIsPure = noClasses().that().resideInAPackage("..core.domain..")
      .should().dependOnClassesThat().resideInAnyPackage("..core.service..", "..core.port..",
          "org.springframework..");
}
