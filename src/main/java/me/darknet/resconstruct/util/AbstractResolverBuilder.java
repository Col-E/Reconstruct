package me.darknet.resconstruct.util;

import me.coley.analysis.TypeResolver;

public abstract class AbstractResolverBuilder {
	private TypeResolver typeResolver;

	public final TypeResolver get() {
		if (typeResolver == null)
			typeResolver = buildTypeResolver();
		return typeResolver;
	}

	protected abstract TypeResolver buildTypeResolver();
}
