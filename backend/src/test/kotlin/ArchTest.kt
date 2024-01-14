package com.hexagonkt.realworld

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import kotlin.test.Test

internal class ArchTest {

    companion object {
        private val APPLICATION_PACKAGE: String = Application::class.java.`package`.name
        private val DOMAIN_PACKAGE: String = "$APPLICATION_PACKAGE.domain"
        private val DOMAIN_MODEL_PACKAGE: String = "$DOMAIN_PACKAGE.model"
        private val ADAPTER_PACKAGE: String = "$APPLICATION_PACKAGE.adapter"

        private val classes: JavaClasses = ClassFileImporter().importPackages(APPLICATION_PACKAGE)
    }

    @Test fun `Domain can only access domain`() {
        classes()
            .that()
            .resideInAPackage("$DOMAIN_PACKAGE..")
            .should()
            .onlyAccessClassesThat()
            .resideInAnyPackage(
                "$DOMAIN_PACKAGE..",
                "java..",
                "kotlin..",
                "com.hexagonkt.store.." // TODO This must be deleted after refactor
            )
            .check(classes)
    }
}
