/*
Copyright 2016, Marko Devcic, madevcic@gmail.com, http://www.markodevcic.com

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.markodevcic.kvalidation

import com.markodevcic.kvalidation.errors.ValidationError
import com.markodevcic.kvalidation.validators.PropertyValidator
import java.util.*

/**
 * Base class for all validators
 * @param T the type of object being validated
 * @param consumer the actual object being validated
 */
abstract class ValidatorBase<T>(private val consumer: T) where T : Any {
    private val contexts: MutableList<PropertyContext<T, *>> = ArrayList()

    var strategy = ValidationStrategy.FULL

    /**
     * Creates a rule builder object for defined property
     * @sample <
     *          //Java
     *          validator.forProperty(SomeObject::getName)
     *              .notNull()
     *              .equal("John")
     *
     *          //Kotlin
     *          validator.forProperty { t -> t.name } rules {
     *              notNull()
     *              equal("John")
     *          }
     * >
     * @param valueFactory lambda that defines a property from the object being validated
     * @return [RuleBuilder] object
     */
    fun <TFor : Any> forProperty(valueFactory: (T) -> TFor?): RuleBuilder<T, TFor> {
        val propertyContext = PropertyContext(valueFactory)
        contexts.add(propertyContext)
        return RuleBuilder(propertyContext)
    }

    /**
     * Validates the object
     * @return [ValidationResult]
     */
    fun validate(): ValidationResult {
        val result = ValidationResult()
        contexts.forEach { context ->
            val value = context.valueFactory(consumer)
            context.validators.forEach { validator ->
                if (validator.precondition?.invoke(consumer) ?: true && !validator.isValid(value)) {
                    val error = createValidationError(validator, value, context.propertyName)
                    result.validationErrors.add(error)
                    if (strategy == ValidationStrategy.STOP_ON_FIRST) {
                        return result
                    }
                }
            }
        }
        return result
    }

    private fun <TFor : Any> createValidationError(propertyValidator: PropertyValidator, value: TFor?, propertyName: String?): ValidationError {
        val debugMessage = "$propertyValidator, received value: ${value ?: "null"}" +
                if (propertyName != null) ", property name: $propertyName" else ""
        val error = ValidationError(propertyValidator.messageBuilder?.getErrorMessage()
                ?: debugMessage, propertyValidator.errorLevel, propertyValidator.errorCode, debugMessage)
        return error
    }
}

infix fun <T, TFor> RuleBuilder<T, TFor>.rules(ruleInit: RuleBuilder<T, TFor>.() -> Unit): RuleBuilder<T, TFor> {
    ruleInit.invoke(this)
    return this
}

infix fun <T, TFor> RuleBuilder<T, TFor>.onError(onErrorInit: OnErrorBuilder<T, TFor>.() -> Unit) {
    onErrorInit(this.onError())
}