import java.util.*;
import java.io.*;

public class Grep {
    public static void main(String[] args)
	throws FileNotFoundException, 
	       MalformedGrammarException,
	       ExceptionInInitializerError,
           Exception {
        if (args.length != 2) {
            System.out.println("Usage: java Grep <regex>");
            System.exit(1);
        }

        String input = args[0];

        /* Build the CFG */ 
        CFG cfg = new CFG("regex-grammar.cfg");
        EarleyParse.setGrammar(cfg);

        /* Parse The Input String */
        ASTNode parsed = null;
        try {
            parsed = EarleyParse.parse(input);
        } catch (NullPointerException e) {
            System.out.println("Your CFG failed to parse the input " + input + "!!");
            System.exit(1);
        }

        NFA N = makeNFAFromRegex(parsed);

        String fileName = args[1];
        Scanner in = new Scanner(new File(fileName));
        NFA newNFA = NFA.concat(NFA.concat(NFA.star(NFA.dot()), N), NFA.star(NFA.dot()));
        while (in.hasNextLine()) {
            String inputLine = in.nextLine();
            if (newNFA.read(inputLine)) {
                System.out.println(inputLine);
            }
        }
    }

    public static NFA makeNFAFromRegex(ASTNode n) {
        if (n.isTerminal()) {
            FSMState newStartState = new FSMState();
            Set<FSMState> newStates = new HashSet<FSMState>();
            newStates.add(newStartState);
            Set<FSMState> newFinalStates = new HashSet<FSMState>();
            Set<FSMTransition> newTransitions = new HashSet<FSMTransition>();

            String value = n.getValue();
            char ch = value.charAt(0);
            // case when the value is empty set
            if (ch == '-') { // case when the value is epsilon
                newFinalStates.add(newStartState);
            } else { // other cases
                FSMState nextState = new FSMState();
                newStates.add(nextState);
                newFinalStates.add(nextState);
                newTransitions.add(new FSMTransition(ch, newStartState, nextState));
            }
            // when ch is empty set, just return an empty nfa
            return new NFA(newStates, newStartState, newFinalStates, newTransitions);
        } else {
            String rule = n.getRuleName();
            // case parenthesis
            if (rule.equals("P")) {
                if (n.hasOneChild()) {
                    return makeNFAFromRegex(n.getChild());
                }
                return makeNFAFromRegex(n.getChildren()[1]);
            } else if (rule.equals("S")){ // case star
                if (n.hasOneChild()) {
                    return makeNFAFromRegex(n.getChild());
                }
                return NFA.star(makeNFAFromRegex(n.getLeftChild()));
            } else if (rule.equals("C")) { // case concat
                // case C->T
                if (n.hasOneChild()) {
                    return makeNFAFromRegex(n.getChild());
                }
                // case C->CT or C->TC
                NFA leftNFA = makeNFAFromRegex(n.getLeftChild());
                NFA rightNFA = makeNFAFromRegex(n.getRightChild());
                return NFA.concat(leftNFA, rightNFA);
            } else { // case union
                if (n.hasOneChild()) {
                    return makeNFAFromRegex(n.getChild());
                }
                NFA leftNFA = makeNFAFromRegex(n.getLeftChild());
                NFA rightNFA = makeNFAFromRegex(n.getRightChild());
                return NFA.union(leftNFA, rightNFA);
            }
        }
    }
}
