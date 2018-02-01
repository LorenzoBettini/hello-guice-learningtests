package com.examples.helloguice;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.google.inject.Scope;
import com.google.inject.ScopeAnnotation;
import com.google.inject.Singleton;

public class CustomScopeGuiceLearningTest {

	/**
	 * Annotation for parameters that will be injected with our factory.
	 * 
	 * @author Lorenzo Bettini
	 *
	 */
	@Target({ TYPE, METHOD })
	@Retention(RUNTIME)
	@ScopeAnnotation
	static @interface FactoryParameterScoped {

	}

	/**
	 * A parameter that can be injected with our factory.
	 * 
	 * @author Lorenzo Bettini
	 *
	 */
	private static interface InjectableParameter {

	}

	@FactoryParameterScoped
	private static class MyParam implements InjectableParameter {
		public int i = 0;

		public MyParam() {

		}

		public MyParam(int i) {
			this.i = i;
		}

	}

	@FactoryParameterScoped
	private static class MyOtherParam implements InjectableParameter {
		public MyOtherParam() {

		}
	}

	private static class FactoryScope implements Scope {

		private static class Parameters extends HashMap<Key<?>, Object> {

			private static final long serialVersionUID = 577780233892248717L;

		}

		private static class ParametersStack extends ArrayDeque<Parameters> {

			private static final long serialVersionUID = 1198277842953694531L;

		}

		// Make this a ThreadLocal for multithreading.
		private final ThreadLocal<ParametersStack> parametersStack = new ThreadLocal<ParametersStack>() {
			@Override
			protected ParametersStack initialValue() {
				return new ParametersStack();
			}
		};

		@Override
		public <T> Provider<T> scope(final Key<T> key, final Provider<T> unscoped) {
			return new Provider<T>() {
				@Override
				public T get() {
					Object param = parametersStack.get().peek().get(key);
					@SuppressWarnings("unchecked")
					T toReturn = (T) param;
					return toReturn;
				}
			};
		}

		public void enter() {
			parametersStack.get().push(new Parameters());
		}

		public void leave() {
			parametersStack.get().pop();
		}

		public void addParameter(InjectableParameter o) {
			parametersStack.get().peek().put(Key.get(o.getClass()), o);
		}

	}

	@Singleton
	private static class MySingleton {

	}

	private static class MyClass {
		@Inject
		private MySingleton mySingleton;

		private MyParam myParam;

		@Inject
		public MyClass(MyInterface field, MyParam myParam) {
			this.myParam = myParam;
		}

		public MyParam getMyParam() {
			return myParam;
		}

		public MySingleton getMySingleton() {
			return mySingleton;
		}

	}

	private static class MyClassCustom extends MyClass {

		@Inject
		public MyClassCustom(MyInterface field, MyParam myParam) {
			super(field, myParam);
		}

	}

	private static class MyOtherClass {
		private MyParam myParam;
		private MyInterface myInterface;

		@Inject
		public MyOtherClass(MyInterface myInterface, MyParam myParam) {
			this.myInterface = myInterface;
			this.myParam = myParam;
		}

		public MyParam getMyParam() {
			return myParam;
		}

		public MyInterface getMyInterface() {
			return myInterface;
		}
	}

	private static interface MyInterface {

	}

	private static class MyImplementation implements MyInterface {

	}

	private static class MyNestedClass extends MyClass {
		private MyClass nested;

		@Inject
		public MyNestedClass(MyInterface field, MyParam myParam, GenericFactory factory) {
			super(field, myParam);
			nested = factory.create(MyClass.class, new MyParam(2));
		}

		public MyClass getNested() {
			return nested;
		}

	}

	private static class MyClassWithSeveralParameters {
		private MyParam myParam;
		private MyOtherParam myOtherParam;

		@Inject
		public MyClassWithSeveralParameters(MyInterface field, MyParam myParam, MyOtherParam myOtherParam) {
			this.myParam = myParam;
			this.myOtherParam = myOtherParam;
		}

		public MyParam getMyParam() {
			return myParam;
		}

		public MyOtherParam getMyOtherParam() {
			return myOtherParam;
		}

	}

	/**
	 * {@link MyParam} is not bound.
	 */
	static class MyModule extends AbstractModule {

		@Override
		protected void configure() {
			FactoryScope scope = new FactoryScope();

			// tell Guice about the scope
			bindScope(FactoryParameterScoped.class, scope);

			// make our scope instance injectable
			bind(FactoryScope.class).toInstance(scope);

			bind(MyInterface.class).to(MyImplementation.class);
		}

	}

	/**
	 * A factory that can inject {@link InjectableParameter} annotated with
	 * {@link FactoryParameterScoped}, using a {@link FactoryScope}.
	 * 
	 * @author Lorenzo Bettini
	 *
	 */
	private static class GenericFactory {
		@Inject
		private Injector injector;

		@Inject
		private FactoryScope scope;

		public <T> T create(Class<T> type, InjectableParameter... injectableParameters) {
			try {
				scope.enter();
				for (InjectableParameter injectableParameter : injectableParameters) {
					scope.addParameter(injectableParameter);
				}
				return injector.getInstance(type);
			} finally {
				scope.leave();
			}
		}
	}

	@Test
	public void testMyScopeIsSingleton() {
		Injector injector = Guice.createInjector(new MyModule());
		FactoryScope scope1 = injector.getInstance(FactoryScope.class);
		FactoryScope scope2 = injector.getInstance(FactoryScope.class);
		assertSame(scope1, scope2);
	}

	@Test
	public void testByDefaultMyParamIsNull() {
		Injector injector = Guice.createInjector(new MyModule());
		FactoryScope scope1 = injector.getInstance(FactoryScope.class);
		scope1.enter();
		assertNull(injector.getInstance(MyParam.class));
	}

	@Test
	public void testCanInjectOtherTypes() {
		Injector injector = Guice.createInjector(new MyModule());
		MyInterface o = injector.getInstance(MyInterface.class);
		assertEquals(MyImplementation.class, o.getClass());
	}

	@Test
	public void testCanInjectSingletons() {
		Injector injector = Guice.createInjector(new MyModule());
		assertSame(injector.getInstance(MySingleton.class), injector.getInstance(MySingleton.class));
	}

	/**
	 * Since the injected MyParam is null
	 */
	@Test(expected = ProvisionException.class)
	public void testCannotInjectTypeThatUsesMyParamWithoutEnteringTheScope() {
		Injector injector = Guice.createInjector(new MyModule());
		injector.getInstance(MyClass.class);
	}

	/**
	 * Since the injected MyParam is NOT null
	 */
	@Test
	public void testCanInjectTypeThatUsesMyParamEnteringTheScope() {
		Injector injector = Guice.createInjector(new MyModule());
		FactoryScope scope1 = injector.getInstance(FactoryScope.class);
		scope1.enter();
		MyParam p1 = new MyParam();
		scope1.addParameter(p1);
		assertSame(p1, injector.getInstance(MyClass.class).getMyParam());
	}

	@Test
	public void testTypesShareSingletons() {
		Injector injector = Guice.createInjector(new MyModule());
		FactoryScope scope1 = injector.getInstance(FactoryScope.class);
		scope1.enter();
		MyParam p1 = new MyParam();
		MyParam p2 = new MyParam();
		scope1.addParameter(p1);
		scope1.addParameter(p2);
		MySingleton mySingleton = injector.getInstance(MySingleton.class);
		assertSame(mySingleton, injector.getInstance(MyClass.class).getMySingleton());
		assertSame(mySingleton, injector.getInstance(MyClass.class).getMySingleton());
	}

	@Test
	public void testCanInjectCustomTypeThatUsesMyParam() {
		Injector injector = Guice.createInjector(new MyModule() {
			@Override
			protected void configure() {
				super.configure();
				bind(MyClass.class).to(MyClassCustom.class);
			}
		});
		FactoryScope scope1 = injector.getInstance(FactoryScope.class);
		scope1.enter();
		MyParam p1 = new MyParam();
		scope1.addParameter(p1);
		MyClass o = injector.getInstance(MyClass.class);
		assertSame(p1, o.getMyParam());
		assertEquals(MyClassCustom.class, o.getClass());
	}

	@Test
	public void testCanInjectWithMyFactory() {
		Injector injector = Guice.createInjector(new MyModule());
		GenericFactory factory = injector.getInstance(GenericFactory.class);
		MyParam p1 = new MyParam();
		MyParam p2 = new MyParam();
		MyClass o1 = factory.create(MyClass.class, p1);
		assertSame(p1, o1.getMyParam());
		MyOtherClass o2 = factory.create(MyOtherClass.class, p2);
		assertSame(p2, o2.getMyParam());
		assertSame(MyImplementation.class, o2.getMyInterface().getClass());
	}

	@Test
	public void testCanInjectNestedWithMyFactory() {
		Injector injector = Guice.createInjector(new MyModule());
		GenericFactory factory = injector.getInstance(GenericFactory.class);
		MyParam p1 = new MyParam();
		MyNestedClass o = factory.create(MyNestedClass.class, p1);
		assertSame(p1, o.getMyParam());
		assertEquals(2, o.getNested().getMyParam().i);
	}

	@Test
	public void testCanInjectCustomTypeWithMyFactory() {
		Injector injector = Guice.createInjector(new MyModule() {
			@Override
			protected void configure() {
				super.configure();
				bind(MyClass.class).to(MyClassCustom.class);
			}
		});
		GenericFactory factory = injector.getInstance(GenericFactory.class);
		MyParam p1 = new MyParam();
		MyParam p2 = new MyParam();
		assertSame(p1, ((MyClassCustom) factory.create(MyClass.class, p1)).getMyParam());
		assertSame(p2, factory.create(MyOtherClass.class, p2).getMyParam());
	}

	@Test
	public void testCanInjectSeveralParametersWithMyFactory() {
		Injector injector = Guice.createInjector(new MyModule());
		GenericFactory factory = injector.getInstance(GenericFactory.class);
		MyParam p1 = new MyParam();
		MyOtherParam p2 = new MyOtherParam();
		MyClassWithSeveralParameters o = factory.create(MyClassWithSeveralParameters.class, p1, p2);
		assertSame(p1, o.getMyParam());
		assertSame(p2, o.getMyOtherParam());
	}

	@Test
	public void testCanInjectSeveralParametersInAnyOrderWithMyFactory() {
		Injector injector = Guice.createInjector(new MyModule());
		GenericFactory factory = injector.getInstance(GenericFactory.class);
		MyParam p1 = new MyParam();
		MyOtherParam p2 = new MyOtherParam();
		// the order does not need to respect the one in the injected constructor
		MyClassWithSeveralParameters o = factory.create(MyClassWithSeveralParameters.class, p2, p1);
		assertSame(p1, o.getMyParam());
		assertSame(p2, o.getMyOtherParam());
	}

	@Test
	public void testMultiThreading() throws Exception {
		Injector injector = Guice.createInjector(new MyModule());
		GenericFactory factory = injector.getInstance(GenericFactory.class);
		List<Thread> threads = new ArrayList<>();
		List<Exception> exceptions = new ArrayList<>();
		for (int i = 0; i < 10000; ++i) {
			Thread thread = new Thread() {
				@Override
				public void run() {
					try {
						factory.create(MyClass.class, new MyParam());
					} catch (ProvisionException e) {
						exceptions.add(e);
					}
				};
			};
			threads.add(thread);
			thread.start();
		}
		for (Thread thread : threads) {
			thread.join();
		}
		if (!exceptions.isEmpty()) {
			throw exceptions.get(0);
		}
	}
}
