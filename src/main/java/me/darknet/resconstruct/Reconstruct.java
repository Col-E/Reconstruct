package me.darknet.resconstruct;

import me.coley.analysis.SimAnalyzer;
import me.coley.analysis.SimInterpreter;
import me.coley.analysis.TypeResolver;
import me.darknet.resconstruct.analysis.StackCopyingSimAnalyser;
import me.darknet.resconstruct.util.AbstractResolverBuilder;
import me.darknet.resconstruct.util.GraphResolverBuilder;
import me.darknet.resconstruct.solvers.InheritanceSolver;
import me.darknet.resconstruct.solvers.InstructionsSolver;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;

public class Reconstruct {
	private final Map<String, ClassReader> inputs = new HashMap<>();
	private final AbstractResolverBuilder graphBuilder = newGraphHelper();
	private ClassHierarchy hierarchy;
	private boolean ignoreSolveExceptions;
	private int api = Opcodes.ASM9;

	/**
	 * New instance.
	 */
	public Reconstruct() {
		reset();
	}

	/**
	 * If solve exceptions are ignored, one class's bytecode that cannot be analyzed will not prevent other classes
	 * from being analyzed for phantom generation processing.
	 *
	 * @param ignoreSolveExceptions
	 * 		Flag to ignore solve exceptions.
	 */
	public void setIgnoreSolveExceptions(boolean ignoreSolveExceptions) {
		this.ignoreSolveExceptions = ignoreSolveExceptions;
	}

	/**
	 * Set the ASM API version for the backing interpreter to use.
	 *
	 * @param api
	 * 		ASM API version.
	 */
	public void setApi(int api) {
		this.api = api;
	}

	/**
	 * Adds a class to the input.
	 * Each input will be scanned for references, of which will be analyzed in {@link #run()}.
	 *
	 * @param classFile
	 * 		Class bytecode.
	 */
	public void add(byte[] classFile) {
		ClassReader cr = new ClassReader(classFile);
		inputs.put(cr.getClassName(), cr);
		hierarchy.createInputPhantom(cr);
		if (graphBuilder instanceof GraphResolverBuilder) {
			((GraphResolverBuilder) graphBuilder).addClass(classFile);
		}
	}

	/**
	 * Clear input information.
	 */
	public void reset() {
		hierarchy = new ClassHierarchy();
		inputs.clear();
	}

	/**
	 * Read input and generate phantom classes.
	 */
	public void run() {
		// Initial pass to generate base phantom types
		for (ClassReader cr : inputs.values())
			cr.accept(new PhantomVisitor(Opcodes.ASM9, null, this), ClassReader.SKIP_FRAMES);

		// Second pass to flesh out phantom types
		for (ClassReader cr : inputs.values()) {
			ClassNode classNode = new ClassNode();
			cr.accept(classNode, ClassReader.EXPAND_FRAMES);
			InstructionsSolver solver = new InstructionsSolver(this);
			try {
				solver.solve(hierarchy, classNode);
			} catch (SolveException ex) {
				if (!ignoreSolveExceptions)
					throw ex;
			}
		}

		// Third pass sort inheritance and do interface solving
		InheritanceSolver solver = new InheritanceSolver();
		solver.solve(hierarchy, null);
	}

	/**
	 * <b>Note:</b> Each method should be given its own analyzer instance.
	 *
	 * @return New analyzer instance.
	 */
	public SimAnalyzer newAnalyzer(NavigableMap<Integer, FrameNode> stackFrames) {
		SimInterpreter interpreter = new SimInterpreter(api);
		SimAnalyzer analyzer = new StackCopyingSimAnalyser(stackFrames, interpreter) {
			@Override
			public TypeResolver createTypeResolver() {
				return graphBuilder.get();
			}
		};
		analyzer.setThrowUnresolvedAnalyzerErrors(false);
		return analyzer;
	}

	/**
	 * @return New graph helper.
	 */
	protected AbstractResolverBuilder newGraphHelper() {
		return new GraphResolverBuilder();
	}

	/**
	 * @return Type resolver.
	 */
	public TypeResolver getTypeResolver() {
		return graphBuilder.get();
	}

	/**
	 * Binary export of {@link #getHierarchy()}.
	 *
	 * @return Map of class names, to generated bytecode.
	 */
	public Map<String, byte[]> build() {
		return hierarchy.export();
	}

	/**
	 * @return Output class hierarchy.
	 */
	public ClassHierarchy getHierarchy() {
		return hierarchy;
	}

	/**
	 * @return Map of inputs classes.
	 */
	public Map<String, ClassReader> getInputs() {
		return inputs;
	}
}
