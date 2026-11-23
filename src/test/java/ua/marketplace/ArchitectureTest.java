package ua.marketplace;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleName;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

@AnalyzeClasses(packages = "ua.marketplace", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String ROOT = "ua.marketplace.";

    @ArchTest
    static final ArchRule no_cycles_between_modules =
        slices().matching("ua.marketplace.(*)..").should().beFreeOfCycles();

    // Кожне правило: модуль НЕ залежить від заборонених модулів.
    @ArchTest
    static final ArchRule common_depends_on_nothing = forbid("common", "seller", "billing", "subscription", "catalog", "advertising", "order", "payment");
    @ArchTest
    static final ArchRule seller_rules = forbid("seller", "billing", "subscription", "catalog", "advertising", "order", "payment");
    @ArchTest
    static final ArchRule billing_rules = forbid("billing", "subscription", "catalog", "advertising", "order", "payment");
    @ArchTest
    static final ArchRule subscription_rules = forbid("subscription", "catalog", "advertising", "order", "payment");
    @ArchTest
    static final ArchRule catalog_rules = forbid("catalog", "seller", "billing", "advertising", "order", "payment");
    @ArchTest
    static final ArchRule advertising_rules = forbid("advertising", "seller", "order", "payment");
    @ArchTest
    static final ArchRule order_rules = forbid("order", "seller", "advertising", "payment");
    @ArchTest
    static final ArchRule payment_rules = forbid("payment", "seller", "catalog", "advertising");

    // billing і subscription можуть залежати від seller ТІЛЬКИ через SellerRegisteredEvent.
    @ArchTest
    static final ArchRule seller_event_only =
        noClasses().that().resideInAnyPackage(ROOT + "billing..", ROOT + "subscription..")
            .should().dependOnClassesThat(
                resideInAPackage(ROOT + "seller..").and(not(simpleName("SellerRegisteredEvent"))));

    @ArchTest
    static final ArchRule controllers_do_not_use_repositories =
        noClasses().that().haveSimpleNameEndingWith("Controller")
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository");

    @ArchTest
    static final ArchRule no_field_injection =
        noFields().should().beAnnotatedWith(Autowired.class);

    // Веб-зріз: залежить від усіх модулів, але ніхто не залежить від нього.
    @ArchTest
    static final ArchRule no_module_depends_on_web =
        noClasses().that().resideOutsideOfPackage(ROOT + "web..")
            .should().dependOnClassesThat().resideInAPackage(ROOT + "web..");

    @ArchTest
    static final ArchRule web_uses_services_not_persistence =
        noClasses().that().resideInAPackage(ROOT + "web..")
            .should().dependOnClassesThat(
                simpleNameEndingWith("Repository").or(CanBeAnnotated.Predicates.annotatedWith(Entity.class)));

    @ArchTest
    static final ArchRule web_does_not_call_rest_controllers =
        noClasses().that().resideInAPackage(ROOT + "web..")
            .should().dependOnClassesThat().areAnnotatedWith(RestController.class);

    @ArchTest
    static final ArchRule rest_controllers_outside_web =
        classes().that().areAnnotatedWith(RestController.class)
            .should().resideOutsideOfPackage(ROOT + "web..");

    @ArchTest
    static final ArchRule web_controllers_are_mvc =
        classes().that().resideInAPackage(ROOT + "web..").and().haveSimpleNameEndingWith("Controller")
            .should().beAnnotatedWith(Controller.class).andShould().notBeAnnotatedWith(RestController.class);

    private static ArchRule forbid(String module, String... forbiddenModules) {
        String[] packages = java.util.Arrays.stream(forbiddenModules).map(m -> ROOT + m + "..").toArray(String[]::new);
        return noClasses().that().resideInAPackage(ROOT + module + "..")
            .should().dependOnClassesThat().resideInAnyPackage(packages);
    }
}
