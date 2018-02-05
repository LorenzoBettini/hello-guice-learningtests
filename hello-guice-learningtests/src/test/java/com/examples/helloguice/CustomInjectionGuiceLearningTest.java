package com.examples.helloguice;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertNotNull;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Collection;

import org.junit.Test;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
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

	/**
	 * {@link TypeListener} for {@link AfterInject} annotations.
	 * 
	 * @author Lorenzo Bettini
	 *
	 */
	static private class AfterInjectTypeListener implements TypeListener {

		/**
		 * This is notified once-per-instance, so it should run as quickly as possible;
		 * in our implementation we have previously collected the list of methods
		 * {@link AfterInject} to call, and this listener is notified only if there are
		 * such methods in the type of the instance.
		 * 
		 * @author Lorenzo Bettini
		 *
		 * @param <I>
		 */
		private static class AfterInjectInjectionListener<I> implements InjectionListener<I> {
			private Collection<Method> afterInjectMethods;

			public AfterInjectInjectionListener(Collection<Method> afterInjectMethods) {
				this.afterInjectMethods = afterInjectMethods;
			}

			@Override
			public void afterInjection(final I injectee) {
				for (final Method method : afterInjectMethods) {
					try {
						method.setAccessible(true);
						method.invoke(injectee);
					} catch (final Exception e) {
						throw new RuntimeException("@AfterInject " + method, e);
					}
				}
			}
		}

		/**
		 * TypeListeners get notified of the types that Guice injects. Since type
		 * listeners are only notified once-per-type, so we can run potentially slow
		 * operations like, in this case, collect all methods annotated with
		 * {@link AfterInject}; if there's no such methods, we don't even register our
		 * {@link AfterInjectInjectionListener}.
		 * 
		 * @see com.google.inject.spi.TypeListener#hear(com.google.inject.TypeLiteral,
		 *      com.google.inject.spi.TypeEncounter)
		 */
		@Override
		public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
			Collection<Method> afterInjectMethods = Collections2.filter(asList(type.getRawType().getMethods()),
					new Predicate<Method>() {
						@Override
						public boolean apply(Method method) {
							return method.isAnnotationPresent(AfterInject.class);
						}
					});
			if (!afterInjectMethods.isEmpty()) {
				encounter.register(new AfterInjectInjectionListener<I>(afterInjectMethods));
			}
		}

		public static void bindAfterInjectTypeListener(Binder binder) {
			binder.bindListener(Matchers.any(), new AfterInjectTypeListener());
		}

	}

	static private class AfterInjectModule extends AbstractModule {

		@Override
		protected void configure() {
			AfterInjectTypeListener.bindAfterInjectTypeListener(binder());
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

	@Test(expected = ProvisionException.class)
	public void testWrongImplementation() {
		Injector injector = Guice.createInjector(new AfterInjectModule());
		injector.getInstance(MyWrongSubclass.class);
	}
}
