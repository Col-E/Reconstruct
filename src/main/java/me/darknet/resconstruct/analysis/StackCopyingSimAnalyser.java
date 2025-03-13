package me.darknet.resconstruct.analysis;

import me.coley.analysis.SimAnalyzer;
import me.coley.analysis.SimFrame;
import me.coley.analysis.SimInterpreter;
import me.coley.analysis.value.AbstractValue;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.NavigableMap;

/**
 * Analyzer implementation that takes in a map of stack-frames in order to enrich analysis in {@link StackCopyingSimFrame}
 */
public class StackCopyingSimAnalyser extends SimAnalyzer {
	private final NavigableMap<Integer, FrameNode> stackFrames;
	private String currentOwner;

	/**
	 * Create analyzer.
	 *
	 * @param stackFrames
	 * 		Map of stack frames supplied by {@code StackMapTable}.
	 * 		Only keyframes included, so we use {@link NavigableMap} to do floor lookups.
	 * @param interpreter
	 * 		Interpreter to use.
	 */
	public StackCopyingSimAnalyser(NavigableMap<Integer, FrameNode> stackFrames, SimInterpreter interpreter) {
		super(interpreter);
		this.stackFrames = stackFrames;
	}

	@Override
	public SimFrame[] analyze(String owner, MethodNode method) throws AnalyzerException {
		this.currentOwner = owner;
		return super.analyze(owner, method);
	}

	@Override
	protected SimFrame newFrame(Frame<? extends AbstractValue> frame) {
		return new StackCopyingSimFrame(currentOwner, stackFrames, (SimFrame) frame);
	}

	@Override
	protected SimFrame newFrame(int numLocals, int numStack) {
		return new StackCopyingSimFrame(currentOwner, stackFrames, numLocals, numStack);
	}
}
