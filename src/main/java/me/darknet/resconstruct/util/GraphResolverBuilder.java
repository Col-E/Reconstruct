package me.darknet.resconstruct.util;

import me.coley.analysis.TypeResolver;
import me.coley.analysis.util.InheritanceGraph;
import me.coley.analysis.util.TypeUtil;
import org.objectweb.asm.Type;

public class GraphResolverBuilder extends AbstractResolverBuilder {
	private final InheritanceGraph graph = InheritanceUtils.getClasspathGraph().copy();

	public void addClass(byte[] classFile) {
		graph.addClass(classFile);
	}

	@Override
	protected TypeResolver buildTypeResolver() {
		return new TypeResolver() {
			@Override
			public boolean isAssignableFrom(Type first, Type second) {
				return first.equals(common(first, second));
			}

			@Override
			public Type common(Type type1, Type type2) {
				String common = graph.getCommon(type1.getInternalName(), type2.getInternalName());
				if (common != null)
					return Type.getObjectType(common);
				return TypeUtil.OBJECT_TYPE;
			}

			@Override
			public Type commonException(Type type1, Type type2) {
				String common = graph.getCommon(type1.getInternalName(), type2.getInternalName());
				if (common != null)
					return Type.getObjectType(common);
				return TypeUtil.EXCEPTION_TYPE;
			}
		};
	}
}
