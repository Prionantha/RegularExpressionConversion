import org.ietf.jgss.GSSManager;

import java.util.Set;
import java.util.HashSet;

public class NFA {
    protected Set<FSMState> states;
    private FSMState startState;
    private Set<FSMState> finalStates;
    private Set<FSMTransition> transitions;
    private static final char EPSILON = '-';
    private static final char TAB = 9;

    /* Main Constructor */
    public NFA(Set<FSMState> states, FSMState startState, Set<FSMState> finalStates, Set<FSMTransition> transitions) {
        if (!states.contains(startState)) {
            throw new IllegalArgumentException("The start state must be in the NFA.");
        }

        if (!states.containsAll(finalStates)) {
        	throw new IllegalArgumentException("The final states must be in the NFA.");
        }
        
        for (FSMTransition t : transitions) {
        	if (!states.contains(t.getSource()) || !states.contains(t.getDestination())) {
        		throw new IllegalArgumentException("The transitions may only use states in the NFA.");
        	}
        }

        this.states = states;
        this.startState = startState;
        this.finalStates = finalStates;
        this.transitions = transitions;
    }

    /* Copy Constructor */
    public NFA(NFA n) {
        this(new HashSet<FSMState>(n.states), n.startState, new HashSet<FSMState>(n.finalStates), new HashSet<FSMTransition>(n.transitions));
    }

    /* @returns a new NFA that accepts any single character. */
    public static NFA dot() {
        HashSet<FSMState> states = new HashSet<FSMState>();
        HashSet<FSMState> finalStates = new HashSet<FSMState>();
        FSMState startState = new FSMState();
        FSMState acceptState = new FSMState();

        states.add(startState);
        states.add(acceptState);

        finalStates.add(acceptState);

		HashSet<FSMTransition> transitions = new HashSet<FSMTransition>();

        for (char c=' '; c <= '~'; c++) {
            transitions.add(new FSMTransition(c, startState, acceptState));
        }
        transitions.add(new FSMTransition(TAB, startState, acceptState));

        return new NFA(states, startState, finalStates, transitions);
    }


    /* @returns true if and only if the NFA accepts s. */
    public boolean read(String s) {
        HashSet<FSMState> currStates = new HashSet<FSMState>();
        currStates.add(startState);
        
        for (int i = 0; i < s.length(); i++) {
            boolean valid = false;
            HashSet<FSMState> nextStates = new HashSet<FSMState>();
            for (FSMState curr: currStates) {
                for (FSMTransition j: transitions) {
                    if (j.getSource().equals(curr) && j.getCharacter() == s.charAt(i)) {
                        valid = true;
                        nextStates.add(j.getDestination());
                    }
                }
            }
            if (!valid) {
                return false;
            }           
            currStates.clear();
            for (FSMState temp: nextStates) {
                currStates.add(temp);
            }
        }
        for (FSMState finalState: currStates) {
            if (finalStates.contains(finalState)) {
                return true;
            }
        }
        return false;
    }

    /* @returns an NFA which accepts the union of a and b */
    public static NFA union(NFA a, NFA b) {

        // construct new states
        Set<FSMState> newStates = new HashSet<FSMState>();
        for (FSMState temp: a.states) {
            newStates.add(temp);
        }
        for (FSMState temp: b.states) {
            newStates.add(temp);
        }

        // construct new startstate
        FSMState head = new FSMState();
        newStates.add(head);

        // construct new finalstates
        Set<FSMState> newFinalStates = new HashSet<FSMState>();
        for (FSMState temp: a.finalStates) {
            newFinalStates.add(temp);
        }
        for (FSMState temp: b.finalStates) {
            newFinalStates.add(temp);
        }

        // construct new transitions
        Set<FSMTransition> newTransitions = new HashSet<FSMTransition>();
        for (FSMTransition temp: a.transitions) {
            newTransitions.add(temp);
        }
        for (FSMTransition temp: b.transitions) {
            newTransitions.add(temp);
        }

        NFA newNFA = new NFA(newStates, head, newFinalStates, newTransitions);
        Set<FSMTransition> epsilonTransitions = new HashSet<FSMTransition>();
        epsilonTransitions.add(new FSMTransition(EPSILON, head, a.startState));
        epsilonTransitions.add(new FSMTransition(EPSILON, head, b.startState));
        return epsilonClosure(newNFA, epsilonTransitions);
    }

    /* @returns an NFA which accepts the concat of a and b */
    public static NFA concat(NFA a, NFA b) {

        // construct the new transitions
        Set<FSMTransition> newTransitions = new HashSet<FSMTransition>();
        for (FSMTransition temp: a.transitions) {
            newTransitions.add(temp);
        }
        for (FSMTransition temp: b.transitions) {
            newTransitions.add(temp);
        }

        // construct the new states
        Set<FSMState> newStates = new HashSet<FSMState>();
        for (FSMState temp: a.states) {
            newStates.add(temp);
        }
        for (FSMState temp: b.states) {
            newStates.add(temp);
        }

        NFA newNFA = new NFA(newStates, a.startState, b.finalStates, newTransitions);
        
        Set<FSMTransition> epsilonTransitions = new HashSet<FSMTransition>();
        for (FSMState state: a.finalStates) {
            epsilonTransitions.add(new FSMTransition(EPSILON, state, b.startState));
        }
        return epsilonClosure(newNFA, epsilonTransitions);
    }

    /* @returns an NFA which accepts the Kleene star of a */
    public static NFA star(NFA n) {
        FSMState newStartState = new FSMState();
        Set<FSMState> newFinalStates = new HashSet<FSMState>(n.finalStates);
        n.states.add(newStartState);
        newFinalStates.add(newStartState);
        Set<FSMTransition> epsilonTransitions = new HashSet<FSMTransition>();
        epsilonTransitions.add(new FSMTransition(EPSILON, newStartState, n.startState));
        for (FSMState temp: n.finalStates) {
            epsilonTransitions.add(new FSMTransition(EPSILON, temp, n.startState));
        }
        NFA newNFA = new NFA(n.states, newStartState, newFinalStates, n.transitions);
        return epsilonClosure(newNFA, epsilonTransitions);
    }

    /* @returns an NFA which is equivalent to n (including all transitions in epsilonTransition) that
     *          does not contain any epsilon transitions */
    public static NFA epsilonClosure(NFA n, Set<FSMTransition> epsilonTransitions) {
        // add epsilon transitions
        Set<FSMTransition> old = new HashSet<FSMTransition>();
        Set<FSMTransition> curr = new HashSet<FSMTransition>(epsilonTransitions);
        while (!old.equals(curr)) {
            old = new HashSet<FSMTransition>(curr);
            for (FSMTransition tran1: old) {
                for (FSMTransition tran2: old) {
                    if (tran2.getSource().equals(tran1.getDestination())) {
                        curr.add(new FSMTransition(EPSILON, tran1.getSource(),
                                tran2.getDestination()));
                    }
                }
            }
        }

        //replace epsilon transitions
        Set<FSMTransition> newTransitions = new HashSet<FSMTransition>(n.transitions);
        Set<FSMState> newFinalStates = new HashSet<FSMState>(n.finalStates);
        
        for (FSMTransition tmpEpsilon: curr) {
            if (n.finalStates.contains(tmpEpsilon.getDestination())) {
                newFinalStates.add(tmpEpsilon.getSource());
            }
            for (FSMTransition tmpTran: n.transitions) {
                if (tmpEpsilon.getDestination().equals(tmpTran.getSource())) {
                    newTransitions.add(new FSMTransition(tmpTran.getCharacter(),
                            tmpEpsilon.getSource(), tmpTran.getDestination()));
                }
            }
        }

        return new NFA(n.states, n.startState, newFinalStates, newTransitions);
    }
}
