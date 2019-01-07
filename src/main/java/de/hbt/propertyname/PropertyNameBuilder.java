package de.hbt.propertyname;

import static org.objectweb.asm.Opcodes.*;

import java.lang.invoke.*;
import java.lang.reflect.*;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;

import org.objectweb.asm.*;
import org.objectweb.asm.Type;

/**
 * Generate the name (as a {@link String}) of a class property or sequence of properties in a type-safe and
 * refactoring-safe way.
 * <p>
 * Examples:
 * 
 * <pre>
 * <code>
 * assertThat(nameOf(Contract::getCustomer)).isEqualTo("customer");
 * assertThat(name(of(Contract::getCustomer).getName())).isEqualTo("customer");
 * assertThat(name(any(of(Contract::getProducts)).getPrice())).isEqualTo("products.price");
 * </code>
 * </pre>
 * <p>
 * See the documentation of the following methods:
 * <ul>
 * <li>{@link #name(Object)}
 * <li>{@link #nameOf(Function)}
 * <li>{@link #of(Function)}
 * <li>{@link #any(Collection)}
 * </ul>
 */
public class PropertyNameBuilder {

	/**
	 * Runtime support class containing methods called by the generated classes.
	 */
	public static class RT {
		public static void appendName(String name) {
			String newName = PROPERTY_NAME.get();
			if (newName == null) {
				newName = name;
			} else {
				newName += "." + name;
			}
			PROPERTY_NAME.set(newName);
		}

		public static Object proxy(Class<?> clazz) {
			return of(clazz);
		}

		public static Object newList(Class<?> clazz) {
			return Collections.singletonList(of(clazz));
		}

		public static Object newSet(Class<?> clazz) {
			return Collections.singleton(of(clazz));
		}

		public static String Object_toString(Object o) {
			return o.getClass().getName() + "@" + Integer.toHexString(Object_hashCode(o));
		}

		public static boolean Object_equals(Object o1, Object o2) {
			return o1 == o2;
		}

		public static int Object_hashCode(Object o) {
			return System.identityHashCode(o);
		}
	}

	private static final MethodHandle Unsafe_defineAnonymousClass;
	private static final MethodHandle ClassLoader_defineClass;
	private static final MethodHandle Lookup_defineClass;
	private static final MethodHandle MethodHandles_privateLookupIn;
	private static final MethodHandle Unsafe_allocateInstance;
	private static final MethodHandles.Lookup thisLookup = MethodHandles.lookup();

	private static final MethodHandle Class_getConstantPoolMH;
	private static final MethodHandle ConstantPool_getSizeMH;
	private static final MethodHandle ConstantPool_getMethodAtMH;
	private static final MethodHandle ConstantPool_getClassAtMH;

	private static final String RT_name = Type.getInternalName(RT.class);
	private static final WeakHashMap<Class<?>, Object> proxies = new WeakHashMap<>();
	private static final WeakHashMap<Class<?>, Class<?>> resolved = new WeakHashMap<>();
	private static final ThreadLocal<String> PROPERTY_NAME = new ThreadLocal<>();

	static {
		MethodHandle Unsafe_defineAnonymousClassMH = null;
		MethodHandle ClassLoader_defineClassMH = null;
		MethodHandle MethodHandles_privateLookupInMH = null;
		MethodHandle Lookup_defineClassMH = null;
		MethodHandle Unsafe_allocateInstanceMH = null;
		Method Unsafe_objectFieldOffset = null;
		Method Unsafe_putBoolean = null;
		Object unsafe = null;
		try {
			Method Lookup_defineClass = MethodHandles.Lookup.class.getDeclaredMethod("defineClass", byte[].class);
			Lookup_defineClassMH = thisLookup.unreflect(Lookup_defineClass);
			Method MethodHandles_privateLookupInMethod = MethodHandles.class.getDeclaredMethod("privateLookupIn",
					Class.class, MethodHandles.Lookup.class);
			MethodHandles_privateLookupInMH = thisLookup.unreflect(MethodHandles_privateLookupInMethod);
		} catch (Exception e) {
			/*
			 * No Lookup.defineClass() or MethodHandles.privateLookupIn(). Probably Java 8 here. That's fine.
			 */
		}
		try {
			Class<?> unsafeClass = ClassLoader.getSystemClassLoader().loadClass("sun.misc.Unsafe");
			Field theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
			theUnsafeField.setAccessible(true);
			unsafe = theUnsafeField.get(null);
			try {
				Method allocateInstanceMethod = unsafeClass.getDeclaredMethod("allocateInstance", Class.class);
				MethodHandle mh = thisLookup.unreflect(allocateInstanceMethod);
				Unsafe_allocateInstanceMH = mh.asType(mh.type().changeParameterType(0, Object.class)).bindTo(unsafe);
			} catch (Exception e) {
				throw new PropertyException("Cannot generate property names", e);
			}
			Method defineAnonymousClassMethod = unsafeClass.getDeclaredMethod("defineAnonymousClass", Class.class,
					byte[].class, Object[].class);
			MethodHandle mh = thisLookup.unreflect(defineAnonymousClassMethod);
			Unsafe_defineAnonymousClassMH = mh.asType(mh.type().changeParameterType(0, Object.class)).bindTo(unsafe);
			Unsafe_objectFieldOffset = unsafeClass.getDeclaredMethod("objectFieldOffset", Field.class);
			Unsafe_putBoolean = unsafeClass.getDeclaredMethod("putBoolean", Object.class, long.class, boolean.class);
		} catch (Exception e) {
			throw new PropertyException("Cannot generate property names", e);
		}
		try {
			if (Lookup_defineClassMH == null) {
				Method defineClassM = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class,
						int.class, int.class);
				defineClassM.setAccessible(true);
				ClassLoader_defineClassMH = MethodHandles.lookup().unreflect(defineClassM);
			}
		} catch (Exception e) {
			throw new PropertyException("Cannot generate property names", e);
		}
		Unsafe_defineAnonymousClass = Unsafe_defineAnonymousClassMH;
		ClassLoader_defineClass = ClassLoader_defineClassMH;
		Lookup_defineClass = Lookup_defineClassMH;
		MethodHandles_privateLookupIn = MethodHandles_privateLookupInMH;
		Unsafe_allocateInstance = Unsafe_allocateInstanceMH;

		MethodHandle Class_getConstantPoolMH_ = null;
		MethodHandle ConstantPool_getSizeMH_ = null;
		MethodHandle ConstantPool_getMethodAtMH_ = null;
		MethodHandle ConstantPool_getClassAtMH_ = null;
		try {
			Class<?> constantPoolClass;
			try {
				constantPoolClass = Class.forName("jdk.internal.reflect.ConstantPool");
			} catch (ClassNotFoundException e) {
				constantPoolClass = Class.forName("sun.reflect.ConstantPool");
			}
			Method Class_getConstantPool = Class.class.getDeclaredMethod("getConstantPool");
			Method ConstantPool_getSize = constantPoolClass.getDeclaredMethod("getSize");
			Method ConstantPool_getMethodAt = constantPoolClass.getDeclaredMethod("getMethodAt", int.class);
			Method ConstantPool_getClassAt = constantPoolClass.getDeclaredMethod("getClassAt", int.class);
			boolean isAtLeastJava12 = Double.valueOf(System.getProperty("java.class.version")) >= 56.0;
			if (isAtLeastJava12) {
				try {
					Class_getConstantPool.setAccessible(true);
					ConstantPool_getSize.setAccessible(true);
					ConstantPool_getMethodAt.setAccessible(true);
					ConstantPool_getClassAt.setAccessible(true);
				} catch (Exception e) {
					throw new PropertyException(
							"When run under JDK12, please add the JVM arguments '--add-opens java.base/jdk.internal.reflect=ALL-UNNAMED'",
							e);
				}
			} else {
				Field AccessibleObject_override = AccessibleObject.class.getDeclaredField("override");
				long overrideOffset = (long) Unsafe_objectFieldOffset.invoke(unsafe, AccessibleObject_override);
				Unsafe_putBoolean.invoke(unsafe, Class_getConstantPool, overrideOffset, true);
				Unsafe_putBoolean.invoke(unsafe, ConstantPool_getSize, overrideOffset, true);
				Unsafe_putBoolean.invoke(unsafe, ConstantPool_getMethodAt, overrideOffset, true);
				Unsafe_putBoolean.invoke(unsafe, ConstantPool_getClassAt, overrideOffset, true);
			}
			Class_getConstantPoolMH_ = thisLookup.unreflect(Class_getConstantPool);
			Class_getConstantPoolMH_ = Class_getConstantPoolMH_
					.asType(Class_getConstantPoolMH_.type().changeReturnType(Object.class));
			ConstantPool_getSizeMH_ = thisLookup.unreflect(ConstantPool_getSize);
			ConstantPool_getSizeMH_ = ConstantPool_getSizeMH_
					.asType(ConstantPool_getSizeMH_.type().changeParameterType(0, Object.class));
			ConstantPool_getMethodAtMH_ = thisLookup.unreflect(ConstantPool_getMethodAt);
			ConstantPool_getMethodAtMH_ = ConstantPool_getMethodAtMH_
					.asType(ConstantPool_getMethodAtMH_.type().changeParameterType(0, Object.class));
			ConstantPool_getClassAtMH_ = thisLookup.unreflect(ConstantPool_getClassAt);
			ConstantPool_getClassAtMH_ = ConstantPool_getClassAtMH_
					.asType(ConstantPool_getClassAtMH_.type().changeParameterType(0, Object.class));
		} catch (Throwable e) {
			throw new PropertyException("Cannot generate property names", e);
		}
		Class_getConstantPoolMH = Class_getConstantPoolMH_;
		ConstantPool_getSizeMH = ConstantPool_getSizeMH_;
		ConstantPool_getMethodAtMH = ConstantPool_getMethodAtMH_;
		ConstantPool_getClassAtMH = ConstantPool_getClassAtMH_;
	}

	private static <T> Class<T> defineClass(ClassLoader cl, Class<?> hostClass, String name, byte[] definition) {
		try {
			if (Unsafe_defineAnonymousClass != null && hostClass != null) {
				return (Class<T>) Unsafe_defineAnonymousClass.invokeExact(hostClass, definition, (Object[]) null);
			}
			if (Lookup_defineClass != null && hostClass != null) {
				return (Class<T>) Lookup_defineClass.invokeExact(
						(MethodHandles.Lookup) MethodHandles_privateLookupIn.invokeExact(hostClass, thisLookup),
						definition);
			}
			return (Class<T>) ClassLoader_defineClass.invokeExact(cl, name.replace('/', '.'), definition, 0,
					definition.length);
		} catch (Throwable e) {
			throw new PropertyException("Could not define class in JVM: " + name, e);
		}
	}

	private static boolean isGetter(Method m) {
		Class<?> ret = m.getReturnType();
		return Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers())
				&& (m.getName().startsWith("get") && ret != void.class && ret != Void.class
						|| (m.getName().startsWith("is")
								&& (m.getReturnType() == boolean.class || m.getReturnType() == Boolean.class)))
				&& m.getParameterCount() == 0 && m.getDeclaringClass() != Object.class;
	}

	private static boolean canProxy(Class<?> clazz) {
		return !clazz.isArray() && !clazz.isAnnotation() && !clazz.isPrimitive() && String.class != clazz
				&& !clazz.isEnum() && !LocalDate.class.isAssignableFrom(clazz) && !Date.class.isAssignableFrom(clazz)
				&& !Number.class.isAssignableFrom(clazz) && !Boolean.class.isAssignableFrom(clazz)
				&& !Collection.class.isAssignableFrom(clazz) && !Map.class.isAssignableFrom(clazz);
	}

	private static String propertyName(Method m) {
		if (m.getName().startsWith("get"))
			return m.getName().substring(3, 4).toLowerCase() + m.getName().substring(4);
		else if (m.getName().startsWith("is"))
			return m.getName().substring(2, 3).toLowerCase() + m.getName().substring(3);
		throw new IllegalArgumentException("Not a getter method: " + m);
	}

	@SuppressWarnings("unchecked")
	private static <T> Class<T> findNonProxyClass(Class<? extends T> clazz) {
		if (clazz.getName().contains("_$$_") || clazz.getName().contains("$HibernateProxy$"))
			return (Class<T>) findNonProxyClass(clazz.getSuperclass());
		return (Class<T>) clazz;
	}

	private static Class<?> collectionElementType(java.lang.reflect.Type t) {
		if (t instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) t;
			return (Class<?>) pt.getActualTypeArguments()[0];
		}
		throw new PropertyException("Unsupported collection element type: " + t, null);
	}

	private static <T, S extends T> Class<?> resolve(Class<S> sub) {
		Class<?> res = resolved.get(sub);
		if (res != null)
			return res;
		Object constantPool;
		try {
			constantPool = Class_getConstantPoolMH.invokeExact(sub);
		} catch (Throwable e) {
			throw new PropertyException("Could not get ConstantPool of " + sub, e);
		}
		Member member = null;
		int cpSize = constantPoolSize(constantPool);
		for (int i = cpSize - 1; member == null && i >= 0; i--) {
			try {
				Member mem = (Member) ConstantPool_getMethodAtMH.invokeExact(constantPool, i);
				if ("valueOf".equals(mem.getName()) || mem.getDeclaringClass().isAssignableFrom(sub))
					continue;
				member = mem;
			} catch (Throwable t) {
			}
		}
		Class<?> mostSpecific = member.getDeclaringClass();
		for (int i = cpSize - 1; i >= 0; i--) {
			try {
				Class<?> clazz = (Class<?>) ConstantPool_getClassAtMH.invokeExact(constantPool, i);
				if (mostSpecific.isAssignableFrom(clazz) && !mostSpecific.equals(clazz)) {
					mostSpecific = clazz;
					break;
				}
			} catch (Throwable t) {
			}
		}
		resolved.put(sub, mostSpecific);
		return mostSpecific;
	}

	private static int constantPoolSize(Object constantPool) {
		try {
			return (int) ConstantPool_getSizeMH.invokeExact(constantPool);
		} catch (Throwable t) {
			throw new PropertyException("Cannot get ConstantPool size of " + constantPool, t);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> T of(Class<T> clazz) {
		if (!canProxy(clazz))
			throw new PropertyException("Cannot proxy " + clazz, null);
		Object proxy = proxies.get(clazz);
		if (proxy == null) {
			proxy = createProxy(clazz);
			proxies.put(clazz, proxy);
		}
		return (T) proxy;
	}

	/**
	 * To be used in conjunction with {@link #name(Object) name()} like so:
	 * <code>name(of(Contract::getCustomer).getLegalName())</code>
	 * <p>
	 * This scheme can be used when selecting multiple properties in a row.
	 * 
	 * @see #name(Object)
	 * 
	 * @param          <T> type of the property owner
	 * @param          <R> type of the property
	 * @param property a method reference of a getter method
	 * @return the object returned by the getter call on a generated proxy
	 */
	public static <T, R> R of(Function<? super T, R> property) {
		@SuppressWarnings("unchecked")
		T t = (T) of(resolve(property.getClass()));
		return property.apply(t);
	}

	/**
	 * To be used in conjunction with {@link #name(Object) name()} and {@link #of(Function) of()} where the last
	 * navigated property is a collection, like so: <code>name(any(of(Contract::getPositions)).getProduct())</code>
	 * <p>
	 * This method must be used when selecting through collections, wrapping each navigated collection in an
	 * {@link #any(Collection)} call.
	 *
	 * @see #name(Object)
	 * @see #of(Function)
	 *
	 * @param      <T> type of the collection element
	 * @param coll the collection property
	 * @return a proxy for the collection element
	 */
	public static <T> T any(Collection<T> coll) {
		return coll.iterator().next();
	}

	/**
	 * To be used in conjunction with {@link #of(Function) of()} like so:
	 * <code>name(of(Contract::getCustomer).getLegalName())</code>
	 * <p>
	 * This scheme is used when selecting multiple properties in a row. When only selecting a single top-level property,
	 * {@link #nameOf(Function)} should be used like so: <code>nameOf(Contract::getCustomer)</code>.
	 * 
	 * @see #of(Function)
	 * @see #nameOf(Function)
	 * 
	 * @param obj the return value of a getter call to return the property name of
	 * @return the name of the selected properties
	 */
	public static String name(Object obj) {
		String ret = PROPERTY_NAME.get();
		PROPERTY_NAME.remove();
		return ret;
	}

	/**
	 * Given a method reference of a getter method, such as via <code>nameOf(Contract::getCustomer)</code>, this method
	 * returns the property name of the referenced getter (in this case "customer").
	 * 
	 * @param        <T> type of the property owner
	 * @param getter a method reference of a getter method
	 * @return the name of the selected property
	 */
	public static <T> String nameOf(Function<? super T, ?> getter) {
		@SuppressWarnings("unchecked")
		T t = (T) of(resolve(getter.getClass()));
		return name(getter.apply(t));
	}

	private static <T> Object createProxy(Class<T> clazz) {
		clazz = findNonProxyClass(clazz);
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		String superInternalName = clazz.getName().replace('.', '/');
		String internalClassName = superInternalName + "_$$_FieldNameClass";
		cw.visit(V1_8, ACC_PUBLIC | ACC_SUPER, internalClassName, null, superInternalName, null);
		generateEquals(cw);
		generateHashCode(cw);
		generateToString(cw);
		for (Method m : clazz.getMethods()) {
			if (!isGetter(m))
				continue;
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, m.getName(), Type.getMethodDescriptor(m), null, null);
			Type retType = Type.getReturnType(m);
			mv.visitLdcInsn(propertyName(m));
			mv.visitMethodInsn(INVOKESTATIC, RT_name, "appendName", "(Ljava/lang/String;)V", false);
			if (retType.getSort() == Type.OBJECT && canProxy(m.getReturnType())) {
				mv.visitLdcInsn(Type.getType(m.getReturnType()));
				mv.visitMethodInsn(INVOKESTATIC, RT_name, "proxy", "(Ljava/lang/Class;)Ljava/lang/Object;", false);
				mv.visitTypeInsn(CHECKCAST, Type.getInternalName(m.getReturnType()));
			} else if (Collection.class.isAssignableFrom(m.getReturnType())) {
				Class<?> elementType = collectionElementType(m.getGenericReturnType());
				Type elemType = Type.getType(elementType);
				if (canProxy(elementType)) {
					mv.visitLdcInsn(elemType);
					String method = Set.class.isAssignableFrom(m.getReturnType()) ? "newSet" : "newList";
					mv.visitMethodInsn(INVOKESTATIC, RT_name, method, "(Ljava/lang/Class;)Ljava/lang/Object;", false);
					mv.visitTypeInsn(CHECKCAST, Type.getInternalName(m.getReturnType()));
				} else {
					generateDefaultValue(mv, elemType);
				}
			} else {
				generateDefaultValue(mv, Type.getType(m.getReturnType()));
			}
			mv.visitInsn(retType.getOpcode(IRETURN));
			mv.visitMaxs(-1, -1);
			mv.visitEnd();
		}
		cw.visitEnd();
		return defineClassAndInstantiate(clazz, cw, internalClassName);
	}

	private static void generateDefaultValue(MethodVisitor mv, Type type) {
		if (type.getSort() == Type.BOOLEAN || type.getSort() == Type.CHAR || type.getSort() == Type.SHORT
				|| type.getSort() == Type.INT || type.getSort() == Type.BYTE)
			mv.visitInsn(ICONST_0);
		else if (type.getSort() == Type.LONG)
			mv.visitInsn(LCONST_0);
		else if (type.getSort() == Type.FLOAT)
			mv.visitInsn(FCONST_0);
		else if (type.getSort() == Type.DOUBLE)
			mv.visitInsn(DCONST_0);
		else
			mv.visitInsn(ACONST_NULL);
	}

	private static Object defineClassAndInstantiate(Class<?> clazz, ClassWriter cw, String internalClassName) {
		Class<?> generatedClass = defineClass(clazz.getClassLoader(), clazz, internalClassName, cw.toByteArray());
		try {
			return Unsafe_allocateInstance.invokeExact(generatedClass);
		} catch (Throwable e) {
			throw new PropertyException("Could not instantiate propxy for " + generatedClass, e);
		}
	}

	private static void generateEquals(ClassVisitor cv) {
		MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "equals", "(Ljava/lang/Object;)Z", null, null);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitMethodInsn(INVOKESTATIC, RT_name, "Object_equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
		mv.visitInsn(IRETURN);
		mv.visitMaxs(-1, -1);
		mv.visitEnd();
	}

	private static void generateHashCode(ClassVisitor cv) {
		MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "hashCode", "()I", null, null);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESTATIC, RT_name, "Object_hashCode", "(Ljava/lang/Object;)I", false);
		mv.visitInsn(IRETURN);
		mv.visitMaxs(-1, -1);
		mv.visitEnd();
	}

	private static void generateToString(ClassVisitor cv) {
		MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "toString", "()Ljava/lang/String;", null, null);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESTATIC, RT_name, "Object_toString", "(Ljava/lang/Object;)Ljava/lang/String;", false);
		mv.visitInsn(ARETURN);
		mv.visitMaxs(-1, -1);
		mv.visitEnd();
	}

}
