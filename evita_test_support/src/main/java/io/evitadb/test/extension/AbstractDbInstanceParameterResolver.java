/*
 *                         _ _        ____  ____
 *               _____   _(_) |_ __ _|  _ \| __ )
 *              / _ \ \ / / | __/ _` | | | |  _ \
 *             |  __/\ V /| | || (_| | |_| | |_) |
 *              \___| \_/ |_|\__\__,_|____/|____/
 *
 *   Copyright (c) 2023
 *
 *   Licensed under the Business Source License, Version 1.1 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   https://github.com/FgForrest/evitaDB/blob/main/LICENSE
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package io.evitadb.test.extension;

import io.evitadb.api.EvitaBase;
import io.evitadb.api.utils.Assert;
import io.evitadb.test.TestConstants;
import io.evitadb.test.annotation.CatalogName;
import io.evitadb.test.annotation.DataSet;
import io.evitadb.test.annotation.UseDataSet;
import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * Abstract ancestor for Evita DB instance parameter resolver for tests.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public abstract class AbstractDbInstanceParameterResolver<E extends EvitaBase<?,?,?,?,?,?>> implements ParameterResolver, BeforeAllCallback, AfterAllCallback {
	private static final String EVITA_INSTANCE = "__evitaInstance";
	private static final String EVITA_DATA_SET_INDEX = "__dataSetIndex";
	private static final String EVITA_CURRENT_DATA_SET = "__currentDataSet";
	private static final String EVITA_CURRENT_SET_RETURN_OBJECT = "__currentDataSetReturnObject";
	protected static final Path STORAGE_PATH = Path.of(System.getProperty("java.io.tmpdir") + File.separator + "evita");

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		// when test is marked with functional test or integration test tag
		if (context.getTags().contains(TestConstants.FUNCTIONAL_TEST) || context.getTags().contains(TestConstants.INTEGRATION_TEST)) {
			// index data set bootstrap methods
			final Map<String, Method> dataSet = new HashMap<>();
			final Class<?> testClass = context.getRequiredTestClass();
			indexTestClass(dataSet, testClass);
			final Store store = getStore(context);
			store.put(EVITA_DATA_SET_INDEX, dataSet);
		}
	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		// when test is marked with functional test or integration test tag
		if (context.getTags().contains(TestConstants.FUNCTIONAL_TEST) || context.getTags().contains(TestConstants.INTEGRATION_TEST)) {
			// always clear evita at the end of the test class
			destroyEvitaInstanceIfPresent(context);
		}
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
		// this implementation can inject Evita DB instance and String catalogName to the test
		return EvitaBase.class.isAssignableFrom(parameterContext.getParameter().getType()) ||
			"catalogName".equals(parameterContext.getParameter().getName()) ||
			ofNullable(extensionContext.getRequiredTestMethod().getAnnotation(UseDataSet.class))
				.orElseGet(() -> getAnnotationOnSuperMethod(extensionContext, UseDataSet.class)) != null;
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
		final Store store = getStore(extensionContext);

		// get catalog name from class annotation or use default
		final String catalogName = ofNullable(extensionContext.getRequiredTestClass().getAnnotation(CatalogName.class))
			.map(CatalogName::value)
			.orElse(TestConstants.TEST_CATALOG);

		// when Evita implementation is required
		final UseDataSet methodUseDataSet = ofNullable(extensionContext.getRequiredTestMethod().getAnnotation(UseDataSet.class))
			.orElseGet(() -> getAnnotationOnSuperMethod(extensionContext, UseDataSet.class));

		final Parameter requestedParam = parameterContext.getParameter();
		if (EvitaBase.class.isAssignableFrom(requestedParam.getType())) {
			final UseDataSet parameterUseDataSet = ofNullable(requestedParam.getAnnotation(UseDataSet.class))
					.orElseGet(() -> getParameterAnnotationOnSuperMethod(parameterContext, extensionContext, UseDataSet.class));
			Assert.isTrue(
				parameterUseDataSet == null || methodUseDataSet == null,
				"UseDataSet annotation can be specified on parameter OR method level, but not both!"
			);
			final UseDataSet useDataSet = ofNullable(methodUseDataSet).orElse(parameterUseDataSet);

			try {
				final E evita;
				// return initialized Evita instance
				if (useDataSet != null) {
					final String currentDataSet = getCurrentDataSet(store);
					final String dataSetToUse = useDataSet.value();
					if (Objects.equals(currentDataSet, dataSetToUse)) {
						// do nothing - reuse current dataset
						evita = getEvitaInstance(store);
					} else {
						// reinitialize Evita DB from scratch
						destroyEvitaInstanceIfPresent(extensionContext);
						evita = createNewEvitaInstance(store, catalogName);
						// call method that initializes the dataset
						final Method dataSetInitMethod = getDataSetIndex(store).get(dataSetToUse);
						if (dataSetInitMethod == null) {
							throw new ParameterResolutionException("Requested data set " + dataSetToUse + " has no initialization method within the class (Method with @DataSet annotation)!");
						} else {
							final Object methodResult;
							try {
								if (dataSetInitMethod.getParameterCount() == 1) {
									methodResult = dataSetInitMethod.invoke(extensionContext.getRequiredTestInstance(), evita);
								} else if (dataSetInitMethod.getParameterCount() == 2) {
									methodResult = dataSetInitMethod.invoke(extensionContext.getRequiredTestInstance(), evita, catalogName);
								} else {
									throw new ParameterResolutionException("Data set init method may have one or two arguments (evita instance / catalog name). Failed to init " + dataSetToUse + ".");
								}
							} catch (InvocationTargetException | IllegalAccessException e) {
								throw new ParameterResolutionException("Failed to set up data set " + dataSetToUse, e);
							}
							// set current dataset to context
							store.put(EVITA_CURRENT_DATA_SET, dataSetToUse);
							if (methodResult != null) {
								store.put(EVITA_CURRENT_SET_RETURN_OBJECT, methodResult instanceof DataCarrier ? methodResult : new DataCarrier(methodResult));
							}
						}
						evita.updateCatalog(catalogName, evitaSessionBase -> { evitaSessionBase.goLiveAndClose(); });
					}
				} else {
					// reinitialize Evita DB from scratch (method doesn't use data set - so it needs to start with empty db)
					destroyEvitaInstanceIfPresent(extensionContext);
					evita = createNewEvitaInstance(store, catalogName);
					evita.updateCatalog(catalogName, evitaSessionBase -> { evitaSessionBase.goLiveAndClose(); });
				}
				if (evita == null) {
					throw new ParameterResolutionException("Evita instance was not initialized yet or current test class is neither functional nor integration test (check tags)!");
				} else {
					return evita;
				}
			} catch (IOException ex) {
				throw new ParameterResolutionException("Failed to initialize Evita instance due to an exception!", ex);
			}
			// when catalog name is required
		} else if ("catalogName".equals(requestedParam.getName())) {
			// return resolved test catalog name
			return catalogName;
		} else if (methodUseDataSet != null && getCurrentDataSetReturnObject(store) != null) {
			final DataCarrier currentDataSetReturnObject = getCurrentDataSetReturnObject(store);
			final Object valueByName = currentDataSetReturnObject.getValueByName(requestedParam.getName());
			if (valueByName != null && requestedParam.getType().isInstance(valueByName)) {
				return valueByName;
			} else {
				final Object valueByType = currentDataSetReturnObject.getValueByType(requestedParam.getType());
				if (valueByType != null) {
					return valueByType;
				}
			}
			throw new ParameterResolutionException("Unrecognized parameter " + parameterContext + "!");
		} else {
			throw new ParameterResolutionException("Unrecognized parameter " + parameterContext + "!");
		}
	}

	@Nullable
	private <T extends Annotation> T getParameterAnnotationOnSuperMethod(ParameterContext parameterContext, ExtensionContext extensionContext, Class<T> annotationClass) {
		final Class<?> testSuperClass = extensionContext.getRequiredTestInstance().getClass().getSuperclass();
		if (Object.class.equals(testSuperClass)) {
			return null;
		}

		final Method testMethod = extensionContext.getRequiredTestMethod();
		Method testMethodOnSuperClass = null;
		for (Method superClassMethod : testSuperClass.getDeclaredMethods()) {
			if (superClassMethod.getName().equals(testMethod.getName())
				&& superClassMethod.getParameterCount() == testMethod.getParameterCount()
				&& allParametersAreCompliant(superClassMethod, testMethod)) {
				testMethodOnSuperClass = superClassMethod;
				break;
			}
		}
		if (testMethodOnSuperClass == null) {
			return null;
		}
		int index = -1;
		for (int i = 0; i < testMethod.getParameters().length; i++) {
			final Parameter parameter = testMethod.getParameters()[i];
			if (parameter.equals(parameterContext.getParameter())) {
				index = i;
				break;
			}
		}
		final Parameter superClassParameter = testMethodOnSuperClass.getParameters()[index];
		return superClassParameter.getAnnotation(annotationClass);
	}

	@Nullable
	private <T extends Annotation> T getAnnotationOnSuperMethod(ExtensionContext extensionContext, Class<T> annotationClass) {
		final Class<?> testSuperClass = extensionContext.getRequiredTestInstance().getClass().getSuperclass();
		if (Object.class.equals(testSuperClass)) {
			return null;
		}

		final Method testMethod = extensionContext.getRequiredTestMethod();
		Method testMethodOnSuperClass = null;
		for (Method superClassMethod : testSuperClass.getDeclaredMethods()) {
			if (superClassMethod.getName().equals(testMethod.getName())
				&& superClassMethod.getParameterCount() == testMethod.getParameterCount()
				&& allParametersAreCompliant(superClassMethod, testMethod)) {
				testMethodOnSuperClass = superClassMethod;
				break;
			}
		}
		if (testMethodOnSuperClass == null) {
			return null;
		} else {
			return testMethodOnSuperClass.getAnnotation(annotationClass);
		}
	}

	private boolean allParametersAreCompliant(Method superClassMethod, Method testMethod) {
		for (int i = 0; i < superClassMethod.getParameters().length; i++) {
			final Parameter superParameter = superClassMethod.getParameters()[i];
			final Parameter thisParameter = testMethod.getParameters()[i];
			if (!superParameter.getType().isAssignableFrom(thisParameter.getType())) {
				return false;
			}
		}
		return true;
	}

	protected E getEvitaInstance(Store store) {
		//noinspection unchecked
		return (E) store.get(EVITA_INSTANCE);
	}

	protected String getCurrentDataSet(Store store) {
		return (String) store.get(EVITA_CURRENT_DATA_SET);
	}

	protected DataCarrier getCurrentDataSetReturnObject(Store store) {
		return (DataCarrier) store.get(EVITA_CURRENT_SET_RETURN_OBJECT);
	}

	protected Map<String, Method> getDataSetIndex(Store store) {
		return (Map<String, Method>) store.get(EVITA_DATA_SET_INDEX);
	}

	protected abstract E createEvita(String catalogName);

	private void indexTestClass(Map<String, Method> dataSet, Class<?> testClass) {
		for (Method declaredMethod : testClass.getDeclaredMethods()) {
			ofNullable(declaredMethod.getAnnotation(DataSet.class))
				.ifPresent(it -> {
					declaredMethod.setAccessible(true);
					dataSet.put(it.value(), declaredMethod);
				});
		}
		if (!Object.class.equals(testClass.getSuperclass())) {
			indexTestClass(dataSet, testClass.getSuperclass());
		}
	}

	private E createNewEvitaInstance(Store store, String catalogName) throws IOException {
		// clear Evita DB directory
		destroyEvitaData();
		// create evita instance and configure test catalog
		final E evita = createEvita(catalogName);
		// store references to thread local variables for use in test
		store.put(EVITA_INSTANCE, evita);
		return evita;
	}

	private void destroyEvitaInstanceIfPresent(ExtensionContext context) throws IOException {
		final Store store = getStore(context);
		// close evita and clear data
		ofNullable(getEvitaInstance(store)).ifPresent(EvitaBase::close);
		destroyEvitaData();
		// clear references in thread locals
		store.remove(EVITA_INSTANCE);
	}

	protected abstract void destroyEvitaData() throws IOException;

	private Store getStore(ExtensionContext context) {
		return context.getRoot().getStore(Namespace.GLOBAL);
	}

}
