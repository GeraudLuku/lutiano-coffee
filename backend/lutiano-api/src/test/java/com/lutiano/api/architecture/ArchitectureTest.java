package com.lutiano.api.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

/** The layout and layering rules of the backend, enforced by the build. */
@AnalyzeClasses(packages = "com.lutiano.api", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
  private static final String CONTROLLER = "..controller..", SERVICE = "..service..", REPOSITORY = "..repository..";

  @ArchTest static final ArchRule everyEndpointReturnsAResponseEntity =
      methods().that().areAnnotatedWith(GetMapping.class).or().areAnnotatedWith(PostMapping.class).or().areAnnotatedWith(PutMapping.class)
          .or().areAnnotatedWith(PatchMapping.class).or().areAnnotatedWith(DeleteMapping.class)
          .should().haveRawReturnType(ResponseEntity.class)
          .because("every response is an explicit ResponseEntity (GenericApiResponse for JSON, raw bytes for downloads)");

  @ArchTest static final ArchRule controllersLiveInTheControllerPackage =
      classes().that().haveSimpleNameEndingWith("Controller").should().resideInAPackage(CONTROLLER);

  @ArchTest static final ArchRule repositoriesLiveInTheRepositoryPackage =
      classes().that().haveSimpleNameEndingWith("Repository").should().resideInAPackage(REPOSITORY);

  @ArchTest static final ArchRule controllersDoNotTouchRepositories =
      noClasses().that().resideInAPackage(CONTROLLER).should().dependOnClassesThat().resideInAPackage(REPOSITORY)
          .because("controllers bind and authorise; rules live in services and SQL in repositories");

  @ArchTest static final ArchRule servicesDoNotDependOnControllers =
      noClasses().that().resideInAPackage(SERVICE).should().dependOnClassesThat().resideInAPackage(CONTROLLER);

  @ArchTest static final ArchRule repositoriesDoNotDependOnServicesOrControllers =
      noClasses().that().resideInAPackage(REPOSITORY).should().dependOnClassesThat().resideInAnyPackage(SERVICE, CONTROLLER);

  @ArchTest static final ArchRule noSqlInControllers =
      noClasses().that().resideInAPackage(CONTROLLER).should().dependOnClassesThat().haveFullyQualifiedName("org.springframework.jdbc.core.JdbcTemplate");

  @ArchTest static final ArchRule requestAndResponseModelsAreRecords =
      classes().that().resideInAnyPackage("..models.request..", "..models.response..").and().areTopLevelClasses().and().areNotInterfaces().should().beRecords();
}
