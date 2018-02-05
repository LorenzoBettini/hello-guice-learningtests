package com.examples.helloguice;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.assertNotNull;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

public class CustomInjectionGuiceLearningTest {

	static private class Foo {

	}

	static private class Bar {

	}

	static private class FooBar {

	}

	/**
	 * Annotation for methods that will be called only after the injection has fully
	 * finished: constructors, fields and methods have been completely injected,
	 * also in subclasses.
	 * 
	 * This is different from standard Guice {@link Inject} methods: super types
	 * methods are called before subtypes fields and methods, thus injected fields
	 * in subclasses are not yet injected.
	 * 
	 * @author Lorenzo Bettini
	 *
	 */
	@Target({ METHOD })
	@Retention(RUNTIME)
	static @interface AfterInject {

	}

	static private abstract class MyClass {
		@Inject
		private Foo foo;
		private Bar bar;
		private FooBar fooBar;

		@Inject
		public MyClass(Bar bar) {
			this.bar = bar;
		}

		/**
		 * This will be called only after all fields and methods are injected (even in
		 * subclasses).
		 */
		@AfterInject
		public void init() {
			fooBar = createFooBar();
		}

		protected abstract FooBar createFooBar();

		public Foo getFoo() {
			return foo;
		}

		public Bar getBar() {
			return bar;
		}

		public FooBar getFooBar() {
			return fooBar;
		}

	}

	static private class MySubclass extends MyClass {
		@Inject
		private Provider<FooBar> fooBarProvider;

		@Inject
		public MySubclass(Bar bar) {
			super(bar);
		}

		@Override
		protected FooBar createFooBar() {
			return fooBarProvider.get();
		}

	}

	static private class MyWrongSubclass extends MyClass {
		@Inject
		public MyWrongSubclass(Bar bar) {
			super(bar);
		}

		@Override
		protected FooBar createFooBar() {
			throw new RuntimeException("intentional");
		}

	}

	static private class AfterInjectTypeListener implements TypeListener {

		@Override
		public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
			encounter.register(new InjectionListener<I>() {

				@Override
				public void afterInjection(final I injectee) {
					Method[] methods = injectee.getClass().getMethods();
					for (final Method method : methods) {
						try {
							if (method.isAnnotationPresent(AfterInject.class))
								method.invoke(injectee);
						} catch (final Exception e) {
							throw new RuntimeException("@AfterInject " + method, e);
						}
					}
				}
			});
		}

	}

	static private class AfterInjectModule extends AbstractModule {

		@Override
		protected void configure() {
			bindListener(Matchers.any(), new AfterInjectTypeListener());
			bind(MyClass.class).to(MySubclass.class);
		}

	}

	@Test
	public void testOk() {
		Injector injector = Guice.createInjector(new AfterInjectModule());
		MyClass o = injector.getInstance(MyClass.class);
		assertNotNull(o.getFoo());
		assertNotNull(o.getBar());
		assertNotNull(o.getFooBar());
	}

	@Test(expected=ProvisionException.class)
	public void testWrongImplementation() {
		Injector injector = Guice.createInjector(new AfterInjectModule());
		injector.getInstance(MyWrongSubclass.class);
	}
}
