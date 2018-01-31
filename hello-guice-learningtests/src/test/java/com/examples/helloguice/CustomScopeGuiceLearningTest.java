package com.examples.helloguice;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;

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

	@Target({ TYPE, METHOD })
	@Retention(RUNTIME)
	@ScopeAnnotation
	static @interface MyScopedAnnotation {

	}

	@MyScopedAnnotation
	private static class MyParam {

	}

	private static class MyScope implements Scope {

		// Make this a ThreadLocal for multithreading.
		private final ThreadLocal<Deque<MyParam>> stack = new ThreadLocal<Deque<MyParam>>() {
			@Override
			protected Deque<MyParam> initialValue() {
				return new ArrayDeque<>();
			};
		};

		@Override
		public <T> Provider<T> scope(final Key<T> key, final Provider<T> unscoped) {
			return new Provider<T>() {
				@Override
				public T get() {
					if (key.getTypeLiteral().getRawType().equals(MyParam.class)) {
						@SuppressWarnings("unchecked")
						T toReturn = (T) stack.get().pop();
						return toReturn;
					}
					return unscoped.get();
				}
			};
		}

		public void enter(MyParam o) {
			stack.get().push(o);
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

		@Inject
		public MyOtherClass(MyInterface field, MyParam myParam) {
			this.myParam = myParam;
		}

		public MyParam getMyParam() {
			return myParam;
		}

	}

	private static interface MyInterface {

	}

	private static class MyImplementation implements MyInterface {

	};

	/**
	 * {@link MyParam} is not bound.
	 */
	static class MyModule extends AbstractModule {

		@Override
		protected void configure() {
			MyScope scope = new MyScope();

			// tell Guice about the scope
			bindScope(MyScopedAnnotation.class, scope);

			// make our scope instance injectable
			bind(MyScope.class).toInstance(scope);

			bind(MyInterface.class).to(MyImplementation.class);
		}

	}

	private static class MyFactory {
		@Inject
		private Injector injector;

		@Inject
		private MyScope scope;

		public <T> T create(Class<T> type, MyParam myParam) {
			scope.enter(myParam);
			return injector.getInstance(type);
		}
	}

	@Test
	public void testMyScopeIsSingleton() {
		Injector injector = Guice.createInjector(new MyModule());
		MyScope scope1 = injector.getInstance(MyScope.class);
		MyScope scope2 = injector.getInstance(MyScope.class);
		assertSame(scope1, scope2);
	}

	/**
	 * The stack is empty so pop throws a {@link NoSuchElementException}
	 */
	@Test(expected = ProvisionException.class)
	public void testByDefaultMyParamIsNull() {
		Injector injector = Guice.createInjector(new MyModule());
		injector.getInstance(MyParam.class);
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
		MyScope scope1 = injector.getInstance(MyScope.class);
		MyParam p1 = new MyParam();
		MyParam p2 = new MyParam();
		scope1.enter(p1);
		scope1.enter(p2);
		// note the stack behavior LIFO
		assertSame(p2, injector.getInstance(MyClass.class).getMyParam());
		assertSame(p1, injector.getInstance(MyClass.class).getMyParam());
	}

	@Test
	public void testTypesShareSingletons() {
		Injector injector = Guice.createInjector(new MyModule());
		MyScope scope1 = injector.getInstance(MyScope.class);
		MyParam p1 = new MyParam();
		MyParam p2 = new MyParam();
		scope1.enter(p1);
		scope1.enter(p2);
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
		MyScope scope1 = injector.getInstance(MyScope.class);
		MyParam p1 = new MyParam();
		scope1.enter(p1);
		MyClass o = injector.getInstance(MyClass.class);
		assertSame(p1, o.getMyParam());
		assertEquals(MyClassCustom.class, o.getClass());
	}

	@Test
	public void testCanInjectWithMyFactory() {
		Injector injector = Guice.createInjector(new MyModule());
		MyFactory factory = injector.getInstance(MyFactory.class);
		MyParam p1 = new MyParam();
		MyParam p2 = new MyParam();
		assertSame(p1, factory.create(MyClass.class, p1).getMyParam());
		assertSame(p2, factory.create(MyOtherClass.class, p2).getMyParam());
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
		MyFactory factory = injector.getInstance(MyFactory.class);
		MyParam p1 = new MyParam();
		MyParam p2 = new MyParam();
		assertSame(p1, ((MyClassCustom) factory.create(MyClass.class, p1)).getMyParam());
		assertSame(p2, factory.create(MyOtherClass.class, p2).getMyParam());
	}

	@Test
	public void testMultiThreading() throws Exception {
		Injector injector = Guice.createInjector(new MyModule());
		MyFactory factory = injector.getInstance(MyFactory.class);
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
