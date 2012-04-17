package org.proofpad;

import java.awt.*;
import java.awt.event.*;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.regex.*;

import javax.swing.*;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.Token;
import org.proofpad.Acl2.OutputEvent;
import org.proofpad.SExpUtils.ExpType;


public class Repl extends JPanel {

	static final Icon infoIcon = new ImageIcon(Repl.class.getResource("/media/info.png"));
	static final Icon promptIcon = new ImageIcon(Repl.class.getResource("/media/prompt.png"));
	static final Icon moreIcon = new ImageIcon(Repl.class.getResource("/media/more.png"));

	public interface HeightChangeListener {
		public void heightChanged(int delta);
	}


	public class StatusLabel extends JLabel {
		private static final long serialVersionUID = -6292618935259682146L;
		static final int size = ProofBar.width;
		public StatusLabel(MsgType msg) {
			setHorizontalAlignment(CENTER);
			setMsgType(msg);
			setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.LIGHT_GRAY));
			setBackground(Color.WHITE);
			setOpaque(true);
			setMinimumSize(new Dimension(size, size));
			setMaximumSize(new Dimension(size, Short.MAX_VALUE));
			setPreferredSize(new Dimension(size, size));
		}
		public StatusLabel() {
			this(null);
		}
		public void setMsgType(MsgType m) {
			if (m == null) return;
			switch(m) {
			case ERROR:
				//setText("\u2715");
				setIcon(ProofBar.errorIcon);
				break;
			case INPUT:
				setIcon(promptIcon);
				break;
			case INFO:
				//setText("i");
				setIcon(infoIcon);
				break;
			case SUCCESS:
				//setText("\u2713");
				setIcon(ProofBar.successIcon);
				break;
			}
		}
	}
	
	public class Pair<T, U> {
		public final T first;
		public final U second;

		public Pair(T first, U second) {
			this.first = first;
			this.second = second;
		}
	}
	
	// Python script to generate this:
	
//	from subprocess import Popen, PIPE
//
//	functions = [
//	  # Functins go here
//	]
//
//	for fun in functions:
//	  acl2 = Popen(['/Users/calebegg/Code/acl2/run_acl2'],
//	      stdin=PIPE, stdout=PIPE);
//	  result = acl2.communicate('(trace! (' + fun + ' :native t))')[0];
//	  if 'ACL2 Error' not in result and 'ABORTING' not in result:
//	    print fun
	
	// Then I trimmed it down to keep output to a sane level.

	private static final String[] functionsToTrace = new String[] {
		"*", 
//		"+", 
//		"-", 
		"/", 
		"/=", 
		"1+", 
		"1-", 
		"<", 
		"<=", 
//		"=", 
		">", 
		">=", 
		"ABS", 
		"ACONS", 
		"ALLOCATE-FIXNUM-RANGE", 
		"ALPHORDER", 
		"APPEND", 
		"ASSOC", 
		"ASSOC-STRING-EQUAL", 
		"ATOM", 
		"ATOM-LISTP", 
		"BINARY-*", 
		"BINARY-+", 
//		"BINARY-APPEND", 
		"BOOLEANP", 
//		"CEILING", 
		"CHAR-CODE",
		"CHAR-DOWNCASE", 
		"CHAR-EQUAL", 
		"CHAR-UPCASE", 
		"CHAR<", 
		"CHAR<=", 
		"CHAR>", 
		"CHAR>=", 
		"CHARACTERP", 
		"CLOSE-INPUT-CHANNEL", 
		"CLOSE-OUTPUT-CHANNEL", 
		"CODE-CHAR", 
		"COERCE", 
		"COMPLEX", 
//		"COMPLEX-RATIONALP", 
		"CONCATENATE", 
		"CONJUGATE",
//		"CONS",
//		"CONSP", 
		"COUNT",
		"DELETE-ASSOC-EQUAL", 
		"DENOMINATOR", 
		"EIGHTH", 
//		"ENDP", 
		"EVENP", 
//		"EXPT", 
		"FIFTH", 
		"FIRST", 
//		"FLOOR", 
		"FOURTH", 
		"IFF", 
		"IMAGPART", 
		"IMPLIES", 
		"INTEGERP", 
		"INTERSECTP-EQUAL", 
//		"LEN", 
		"LENGTH", 
		"LIST", 
		"LIST*", 
		"MAX", 
//		"MEMBER", 
//		"MEMBER-EQUAL", 
		"MIN", 
		"MINUSP", 
		"MOD", 
//		"MV-NTH", 
//		"NATP", 
		"NINTH", 
//		"NOT", 
		"NTH", 
		"NTHCDR", 
		"NUMERATOR", 
//		"ODDP", 
		"PLUSP", 
		"POSP", 
		"RATIONAL-LISTP", 
		"READ-BYTE$", 
		"READ-CHAR$", 
//		"READ-OBJECT", 
		"REALPART", 
		"REMOVE", 
		"REST", 
		"REVAPPEND", 
		"RFIX", 
		"SEARCH", 
		"SECOND", 
		"SET-DIFFERENCE-EQUAL", 
		"SETENV$", 
		"SEVENTH", 
		"SIGNUM", 
		"SIXTH", 
		"STRING-APPEND", 
		"STRING-DOWNCASE", 
		"STRING-EQUAL", 
		"STRING-LISTP", 
		"STRING-UPCASE", 
//		"STRING<", 
		"STRING<=", 
		"STRING>", 
		"STRING>=", 
		"STRINGP", 
		"SUBSETP", 
		"SUBSETP-EQUAL", 
		"SYMBOLP", 
		"TAKE", 
		"TENTH", 
		"THIRD", 
		"TRUE-LIST-LISTP", 
//		"TRUE-LISTP", 
		"UNION-EQUAL", 
		"XOR", 
		"ZEROP", 
		"ZIP", 
		"ZP", 
		"ZPF",
	};
	
	static final String addTrace;
	static {
		StringBuilder addTraceBuilder = new StringBuilder("(trace");
		for (String fun : functionsToTrace) {
			addTraceBuilder.append(" " + fun);
		}
		addTraceBuilder.append(")");
		addTrace = addTraceBuilder.toString();
	}
	static final String unTrace;
	static {
		StringBuilder unTraceBuilder = new StringBuilder("(untrace");
		for (String fun : functionsToTrace) {
			unTraceBuilder.append(" " + fun);
		}
		unTraceBuilder.append(")");
		unTrace = unTraceBuilder.toString();
	}
	private static final long serialVersionUID = -4551996064006604257L;
	final Acl2 acl2;
	private JPanel output;
	JScrollBar vertical;
	final ArrayList<Pair<String,Integer>> history;
	private CodePane definitions;
	protected int historyIndex = 0;
	boolean addedInputToHistory = false;
	private Font font;
	private List<JComponent> fontChangeList = new LinkedList<JComponent>();
	CodePane input;
	private int inputHeightOneLine = -1;
	private JScrollPane inputScroller;
	private JSplitPane split;
	private HeightChangeListener heightChangeListener;
	protected int inputLines = 1;
	private JPanel bottom;
	IdeWindow parent;
	protected JButton trace;
	protected JButton run;
		
	enum MsgType {
		ERROR,
		INPUT,
		INFO,
		SUCCESS
	}
	
	public Repl(final IdeWindow parent, Acl2 newAcl2, final CodePane definitions) {
		super();
		this.parent = parent;
		split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false);
		split.setBorder(BorderFactory.createEmptyBorder());
		split.setDividerSize(0);
		split.setResizeWeight(1);
		setLayout(new BorderLayout());
		add(split, BorderLayout.CENTER);
		acl2 = newAcl2;
		acl2.setOutputEventListener(new Acl2.OutputEventListener() {
			@Override
			public void handleOutputEvent(OutputEvent e) {
				displayResult(e.output, e.type);
			}
		});
		this.definitions = definitions;
		setBackground(Color.WHITE);
		setOpaque(true);
		history = new ArrayList<Pair<String, Integer>>();
		output = new JPanel();
		output.setLayout(new BoxLayout(output, BoxLayout.Y_AXIS));
		output.setBackground(Color.WHITE);
		font = new Font("Monospaced", Font.PLAIN, 14);
		output.setFont(font);
		final JScrollPane scroller = new JScrollPane(getOutput());
		vertical = scroller.getVerticalScrollBar();
		scroller.setBorder(BorderFactory.createEmptyBorder());
		scroller.setPreferredSize(new Dimension(500,100));
		split.setTopComponent(scroller);
		bottom = new JPanel();
		bottom.setLayout(new BoxLayout(bottom, BoxLayout.X_AXIS));
		bottom.setBackground(Color.WHITE);
		JLabel prompt = new StatusLabel(MsgType.INPUT);
		input = new CodePane(null);
		input.setDocument(new IdeDocument(null));
		prompt.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				input.requestFocus();
			}
		});
		bottom.add(prompt);
		//input.setFont(font);
		inputScroller = new JScrollPane(input);
		inputScroller.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		//final int inputBorder = 4;
		//input.setBorder(BorderFactory.createEmptyBorder(inputBorder, inputBorder, inputBorder, inputBorder));
		inputScroller.setMaximumSize(new Dimension(Integer.MAX_VALUE, StatusLabel.size + 6));
		bottom.add(inputScroller);
		run = new JButton("run");
		trace = new JButton("trace");
		run.setEnabled(false);
		trace.setEnabled(false);
		//run.putClientProperty("JButton.buttonType", "textured");
		run.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				runInputCode();
			}
		});
		trace.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final String inputText = input.getText();
				displayResult(inputText + "\n", MsgType.INPUT);
				traceExp(inputText);
				history.add(new Pair<String, Integer>(inputText.trim(), inputLines));
				historyIndex = history.size();
				addedInputToHistory = false;
				resetInput();
			}
		});
		input.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				maybeEnableButtons();
				if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE && input.getText().equals("")) {
					// Prevent that awful backspace beep.
					e.consume();
				}
				if (e.getKeyCode() == KeyEvent.VK_UP) {
					if (input.getCaretLineNumber() != 0) {
						return;
					}
					// TODO: Flash background color or something to indicate that the contents has changed? Maybe?
					if (!input.getText().equals("") && !addedInputToHistory) {
						history.add(new Pair<String, Integer>(input.getText(), inputLines));
						addedInputToHistory = true;
					}
					if (historyIndex > 0) {
						historyIndex--;
						Pair<String, Integer> historyEntry = history.get(historyIndex);
						input.setText(historyEntry.first);
						inputLines = historyEntry.second;
						setBottomLines(Math.min(historyEntry.second, 5));
						input.setCaretPosition(0);
					}
				} else if (e.getKeyCode() == KeyEvent.VK_DOWN){
					if (input.getCaretLineNumber() != input.getLineCount() - 1) {
						return;
					}
					if (historyIndex < history.size()) {
						historyIndex++;
						if (historyIndex == history.size()) {
							resetInput();
						} else {
							Pair<String, Integer> historyEntry = history.get(historyIndex);
							input.setText(historyEntry.first);
							inputLines = historyEntry.second;
							setBottomLines(Math.min(historyEntry.second, 5));
							input.setCaretPosition(input.getText().length());
						}
					}
				}
			}
			@Override
			public void keyTyped(KeyEvent e) {
				if (e.getKeyChar() == '\n') {
					int parenLevel = 0;
					for (Token t : input) {
						if (t == null || t.type == Token.NULL) {
							break;
						}
						if (t.isSingleChar('(')) {
							parenLevel++;
						} else if (t.isSingleChar(')')) {
							parenLevel--;
						}
					}
					if (parenLevel <= 0) {
						e.consume();
						runInputCode();
					} else {
						inputLines++;
						if (inputLines <= 6) {
							setBottomLines(inputLines);
						}
					}
				}
				maybeEnableButtons();
			}
		});
		bottom.add(run);
		bottom.add(trace);
		JPanel bottomWrapper = new JPanel();
		bottomWrapper.setLayout(new BorderLayout());
		bottomWrapper.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
		bottomWrapper.add(bottom, BorderLayout.CENTER);
		split.setBottomComponent(bottomWrapper);
	}
	
	void setBottomLines(int inputLines) {
		if (inputHeightOneLine == -1) {
			inputHeightOneLine = input.getHeight();
		}
		int oldDividerLoc = split.getDividerLocation();
		int oldBottomDivisionHeight = split.getHeight() - oldDividerLoc;
		split.setDividerLocation(split.getHeight() - (inputHeightOneLine + 
				(oldBottomDivisionHeight - input.getHeight()) + (inputLines - 1) * input.getLineHeight()));
		bottom.setSize(bottom.getWidth(), inputHeightOneLine + (inputLines - 1) * input.getLineHeight());
		int oldScrollerHeight = inputScroller.getHeight();
		int newScrollerHeight = inputHeightOneLine +
				(inputLines - 1) * input.getLineHeight() + 8;
		newScrollerHeight -= 6;
		Dimension newScrollSize = new Dimension(Short.MAX_VALUE,
				newScrollerHeight);
		inputScroller.setMaximumSize(newScrollSize);
		fireHeightChangedEvent(newScrollerHeight - oldScrollerHeight);
	}
	
	protected void fireHeightChangedEvent(int delta) {
		if (delta == 0) return;
		if (heightChangeListener != null) {
			heightChangeListener.heightChanged(delta);
		}
	}

	void runInputCode() {
		List<Expression> exps = SExpUtils.topLevelExps((RSyntaxDocument) input.getDocument());
		if (exps.size() > 0 && exps.get(0).firstType == ExpType.UNDOABLE) {
			displayResult("This event was moved up to the definitions window.", MsgType.INFO);
			definitions.admitBelowProofLine(input.getText());
			resetInput();
			return;
		}
		displayResult(input.getText() + "\n", MsgType.INPUT);
		acl2.admit(input.getText(), null);
		history.add(new Pair<String, Integer>(input.getText().trim(), inputLines));
		if (history.size() > 500) {
			for (int i = history.size(); i > 400; i--) {
				history.remove(i);
			}
		}
		historyIndex = history.size();
		addedInputToHistory = false;
		resetInput();
	}

	public JPanel getOutput() {
		return output;
	}
	
	void resetInput() {
		input.setText("");
		if (inputHeightOneLine == -1) return;
		inputLines = 1;
		setBottomLines(1);
	}

	private static Pattern welcomeMessage = Pattern.compile(".*ACL2 comes with ABSOLUTELY NO WARRANTY\\..*");
	private static Pattern guardViolation = Pattern.compile("ACL2 Error in TOP-LEVEL: The guard for the function call (.*?), which is (.*?), is violated by the arguments in the call (.*?)\\..*");
	private static Pattern globalVar = Pattern.compile("ACL2 Error in TOP-LEVEL: Global variables, such as (.*?).*?, are not allowed. See :DOC ASSIGN and :DOC @.");
	private static Pattern wrongNumParams = Pattern.compile("ACL2 Error in TOP-LEVEL: (.*?) takes (.*?) arguments? but in the call (.*?) it is given (.*?) arguments?\\..*");
	private static Pattern nonRec = Pattern.compile("Since (.*?) is non-recursive, its admission is trivial\\..*");
	private static Pattern trivial = Pattern.compile("The admission of (.*?) is trivial, using the relation O< .*");
	private static Pattern admission = Pattern.compile("For the admission of (.*?) we will use the relation O< .*");
	private static Pattern proved = Pattern.compile("Q.E.D.");
	// TODO: Add these error messages (and others)
	// Undefined var
	// Redefinition of func/reserved name
	private static Pattern undefinedFunc = Pattern.compile("ACL2 Error in TOP-LEVEL:  The symbol (.*?) \\(in package \"ACL2\"\\) has neither a function nor macro definition in ACL2\\.  Please define it\\..*");
	public static String cleanUpMsg(String result) {
		return cleanUpMsg(result, null, null);
	}
	private static String cleanUpMsg(String result, Set<String> functions, MsgType msgtype) {
		String ret;
		Matcher match;
		String joined = result.replaceAll("[\n\r]+", " ").replaceAll("\\s+", " ").trim();
		if ((match = welcomeMessage.matcher(joined)).matches()) {
			ret = "ACL2 started successfully.";
		} else if ((match = guardViolation.matcher(joined)).matches()) {
			ret = "Guard violation in " + match.group(3).toLowerCase() + ".";
		} else if ((match = globalVar.matcher(joined)).matches()) {
			ret = "Global variables, such as " + match.group(1).toLowerCase() +
					", are not allowed.";
		} else if ((match = wrongNumParams.matcher(joined)).matches()) {
			ret = match.group(1).toLowerCase() +  " takes " + match.group(2) +
					" arguments but was given " + match.group(4) + " at " +
					match.group(3).toLowerCase();
		} else if ((match = trivial.matcher(joined)).matches() ||
				   (match = nonRec.matcher(joined)).matches() ||
				   (match = admission.matcher(joined)).matches()) {
			if (msgtype == MsgType.ERROR) {
				ret = "<html>Admission of <b>" + match.group(1).toLowerCase() + "</b> failed. " +
						"Click for details.</html>";
			} else {
				ret = "<html><b>" + match.group(1).toLowerCase() + "</b> was admitted successfully." +
						"</html>";
			}
		} else if ((match = undefinedFunc.matcher(joined)).matches()) {
			String func = match.group(1).toLowerCase();
//			if (functions != null && functions.contains(func)) {
//				ret = "<html><b>" + func + "</b> must be admitted first. Click the grey bar to " +
//						"the left of its definition.</html>";
//			} else {
				ret = "The function " + func + " is undefined.";
//			}
		} else if ((match = proved.matcher(joined)).find()) {
			ret = "Proof successful.";
		} else if (joined.length() > 70) {
			ret = joined.substring(0, 67) + " ...";
		} else {
			ret = joined;
		}
		return ret;
	}
	public void displayResult(final String result, MsgType type) {
		String traceFreeResult = result.replaceAll("\\s*\\d+>.*?\\n", "").replaceAll("\\s*<\\d+.*?\\n", "");
		String shortResult = cleanUpMsg(traceFreeResult,
				((Acl2Parser) definitions.getParser(0)).functions,
				type);
		if (shortResult.startsWith("ACL2 started successfully")) {
			type = MsgType.INFO;
		}
		final JPanel line = new JPanel();
		line.setPreferredSize(new Dimension(200, 25));
		line.setMaximumSize(new Dimension(Short.MAX_VALUE, 25));
		line.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
		JLabel text = new JLabel(shortResult.trim());
		text.setBackground(Color.LIGHT_GRAY);
		text.setOpaque(false);
		text.setFont(definitions.getFont());
		line.setLayout(new BoxLayout(line, BoxLayout.X_AXIS));
		line.setBackground(Color.WHITE);
		StatusLabel status = new StatusLabel();
		status.setFont(definitions.getFont());
		fontChangeList.add(status);
		fontChangeList.add(text);

		text.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 5));
		line.add(status);
		status.setMsgType(type);
		switch (type) {
		case ERROR:
			line.setBackground(ProofBar.errorColor);
			status.setBackground(ProofBar.errorColor);
			status.setFont(status.getFont().deriveFont(18f));
			break;
		case INFO:
			break;
		case SUCCESS:
			status.setBackground(ProofBar.provedColor);
			status.setFont(status.getFont().deriveFont(18f));
			break;
		case INPUT:
			status.setForeground(Color.GRAY);
			text.setForeground(Color.GRAY);
			break;
		}
		text.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
		line.add(text);
		if (!shortResult.equals(result.trim())) {
			line.add(new JLabel(moreIcon));
			line.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent arg0) {
					JTextArea resBox = new JTextArea();
					resBox.setText(result);
					resBox.setEditable(false);
					parent.setPreviewComponent(resBox);
				}
			});
		}
		synchronized (this) {
			getOutput().add(line);
		}
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				line.scrollRectToVisible(new Rectangle(line.getLocation(), line.getSize()));
				vertical.setValue(vertical.getMaximum());
			}
		});
	}
	

	@Override
	public void setFont(Font f) {
		super.setFont(f);
		if (fontChangeList == null) return;
		for (JComponent c : fontChangeList) {
			c.setFont(f);
		}
		input.setFont(f);
		int size = (25 - input.getLineHeight()) / 2;
		input.setBorder(BorderFactory.createEmptyBorder(size, 10, size, size));
	}

	public void setHeightChangeListener(HeightChangeListener heightChangeListener) {
		this.heightChangeListener = heightChangeListener;
	}

	public int getInputHeight() {
		return inputScroller.getHeight();
	}

	void maybeEnableButtons() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				boolean enable = input.getLastVisibleOffset() != 0;
				run.setEnabled(enable);
				trace.setEnabled(enable);
			}
		});
	}

	void traceExp(final String inputText) {
		String funs = "";
		// Add trace to lots of functions
		for (String fun : ((Acl2Parser) definitions.getParser(0)).functions) {
			funs += " " + fun;
		}
		acl2.admit("(trace$" + funs + ")", Acl2.doNothingCallback);
		acl2.admit(":q", Acl2.doNothingCallback);
		acl2.admit(addTrace, Acl2.doNothingCallback);
		acl2.admit("(lp)", Acl2.doNothingCallback);
		// Run the code
		acl2.admit(inputText, new Acl2.Callback() {
			@Override
			public boolean run(boolean success, String response) {
				// Display the results in a nicely-formatted way
				TraceResult tr = new TraceResult(response, inputText);
				parent.setPreviewComponent(tr);
				return true;
			}
		});
		acl2.admit(":q", Acl2.doNothingCallback);
		acl2.admit(unTrace, Acl2.doNothingCallback);
		acl2.admit("(lp)", Acl2.doNothingCallback);
		acl2.admit("(untrace$" + funs + ")", Acl2.doNothingCallback);
	}
}
