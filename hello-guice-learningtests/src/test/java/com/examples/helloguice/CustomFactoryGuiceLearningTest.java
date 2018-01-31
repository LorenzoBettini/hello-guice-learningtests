package com.examples.helloguice;

import static org.junit.Assert.assertSame;

import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

public class CustomFactoryGuiceLearningTest {

	@Singleton
	private static class MyParam {

	}

	private static class MyClass {
		private MyParam myParam;

		@Inject
		public MyClass(MyInterface field, MyParam myParam) {
			this.myParam = myParam;
		}

		public MyParam getMyParam() {
			return myParam;
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
			bind(MyInterface.class).to(MyImplementation.class);
		}

	}
	

	private static class MyFactory {
		@Inject
		private Injector parentInjector;

		public <T> T create(Class<T> type, MyParam myParam) {
			return parentInjector.createChildInjector(new AbstractModule() {
				@Override
				protected void configure() {
					bind(MyParam.class).toInstance(myParam);
				}
			}).getInstance(type);
		}
	}

	@Test
	public void testMyFactory() {
		MyFactory factory = Guice.createInjector(new MyModule()).getInstance(MyFactory.class);
		MyParam p1 = new MyParam();
		MyParam p2 = new MyParam();
		assertSame(factory.create(MyClass.class, p1).getMyParam(), p1);
		assertSame(factory.create(MyClass.class, p2).getMyParam(), p2);
	}

	@Test
	public void testMyFactoryWithDifferentTypes() {
		MyFactory factory = Guice.createInjector(new MyModule()).getInstance(MyFactory.class);
		MyParam p1 = new MyParam();
		MyParam p2 = new MyParam();
		assertSame(factory.create(MyClass.class, p1).getMyParam(), p1);
		assertSame(factory.create(MyOtherClass.class, p2).getMyParam(), p2);
	}

	/**
	 * It fails because when binding MyClass a JIT binding for MyParam is
	 * automatically created in the parent injector.
	 */
	@Test(expected=CreationException.class)
	public void testMyFactoryWithCustomImplementationOfCreatedType() {
		MyFactory factory = Guice.createInjector(
			new MyModule() {
				@Override
				protected void configure() {
					super.configure();
					bind(MyClass.class).to(MyClassCustom.class);
				};
			}
		).getInstance(MyFactory.class);
		MyParam p1 = new MyParam();
		MyParam p2 = new MyParam();
		assertSame(factory.create(MyClass.class, p1).getMyParam(), p1);
		assertSame(factory.create(MyClass.class, p2).getMyParam(), p2);
	}

	/**
	 * Don't create MyParam with the parent injector or it will create
	 * a JIT binding which will conflict with the child injector:
	 * A just-in-time binding to com.examples.helloguice.CustomFactoryGuiceLearningTest$MyParam was already configured on a parent injector.
	 */
	@Test(expected=CreationException.class)
	public void testMyFactoryFailsBecauseParentHasAJustInTimeBinding() {
		Injector parentInjector = Guice.createInjector(new MyModule());
		parentInjector.getInstance(MyParam.class);
		MyFactory factory = parentInjector.getInstance(MyFactory.class);
		factory.create(MyClass.class, new MyParam());
	}

}
