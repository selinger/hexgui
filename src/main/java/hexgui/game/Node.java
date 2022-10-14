//----------------------------------------------------------------------------
// $Id$ 
//----------------------------------------------------------------------------

package hexgui.game;

import hexgui.hex.HexColor;
import hexgui.hex.HexPoint;
import hexgui.hex.Move;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

//----------------------------------------------------------------------------

/** Node in a game tree.
    Stores moves and other properties.
*/
public class Node
{
    /** Initializes an empty node with a null move. */
    public Node()
    {
	this(null);
    }

    /** Constructs a new node with the specified move.
	@param move move to initialize the node with.
    */
    public Node(Move move)
    {
        // Properties. This can include unstructured properties found
        // in SGF files (i.e., properties that HexGUI doesn't know
        // about), as well as structured properties such as C
        // (comment), PL (player to move), and maybe
        // others. Properties that can take multiple values are not
        // stored here; e.g., LB is stored in m_label.
	m_property = new TreeMap<String, String>();

        // For setup moves.
        m_setup = new TreeMap<HexPoint,HexColor>();

        // A list of cell:label pairs can be attached to a node. This
        // corresponds to the SGF LB property.
        m_label = new Vector<String>();

        // When navigating the tree, the "recent" child of each parent
        // is the one that the "forward" button will navigate to.
        m_recent = false;

        // This node's move.
	setMove(move);
    }

    public void setMove(Move move) { m_move = move;  }
    public Move getMove() { return m_move; }
    public boolean hasMove() { return m_move != null; }

    public void setParent(Node parent) { m_parent = parent; }
    public Node getParent() { return m_parent; }

    public void setPrev(Node prev) { m_prev = prev; }
    public Node getPrev() { return m_prev; }

    public void setNext(Node next) { m_next = next; }
    public Node getNext() { return m_next; }

    /** Sets the first child of this node.  
	This does not update the sibling pointers of the child.
    */
    public void setFirstChild(Node child) { 
	m_child = child; 
    }

    /** Removes this node from the gametree. */
    public void removeSelf()
    {
	Node prev = getPrev();
	Node next = getNext();

        if (prev == null) { 
            // need to fix parent since we're first child
	    if (getParent() != null) {
                getParent().setFirstChild(next);
            }
	} else {
	    prev.setNext(next);
        }

        if (next != null) {
            next.setPrev(prev);
        }
    }

    /** Moves this node to the start of its sibling list. */
    public void moveToFirst()
    {
        Node parent = getParent();
        if (parent == null) {
            return;
        }
        this.removeSelf();
        parent.addFirstChild(this);
    }

    /** Moves this node and all of its parents to the start of their
     * sibling lists */
    public void makeMain()
    {
        Node node = this;
        while (node != null) {
            node.moveToFirst();
            node = node.getParent();
        }
    }

    /** Adds a child to the beginning of the list of children. 
        @param child Node to be added to start of list.
    */     
    public void addFirstChild(Node child) 
    {
        Node oldfirst = m_child;
        m_child = child;
	child.setParent(this);
	child.setPrev(null);
        child.setNext(oldfirst);
        if (oldfirst != null) {
            oldfirst.setPrev(child);
        }
    }
    
    /** Adds a child to the end of the list of children. 
        @param child Node to be added to end of list.
    */     
    public void addChild(Node child) 
    {
	child.setNext(null);
	child.setParent(this);
        
	if (m_child == null) {
	    m_child = child;
	    child.setPrev(null);
	} else {
	    Node cur = m_child;
	    while (cur.getNext() != null)
		cur = cur.getNext();
	    cur.setNext(child);
	    child.setPrev(cur);
	}
    }

    public boolean hasChild()
    {
        return m_child != null;
    }
    
    /** Returns the number of children of this node. */
    public int numChildren()
    {
	int num = 0;
	Node cur = m_child;
	while (cur != null) {
	    num++;
	    cur = cur.getNext();
	}	
	return num;
    }

    /** Returns the nth child. 
	@param n The number of the child to return. 
	@return  The nth child or <code>null</code> that child does not exist.
    */
    public Node getChild(int n) 
    {
	Node cur = m_child;
	for (int i=0; cur != null; i++) {
	    if (i == n) return cur;
	    cur = cur.getNext();
	}
	return null;
    }

    /** Mark the current node as the most recently used among its
     * siblings. This also unmarks the siblings */
    public void markRecent()
    {
        Node parent = getParent();
        if (parent != null) {
            int n = parent.numChildren();
            for (int i=0; i<n; i++) {
                parent.getChild(i).setRecent(false);
            }
        }
        this.setRecent(true);
    }
    
    /** Set the "recent" property of this node. In a list of
     * variations, the recent one is the most recently used. It is the
     * variation that the "forward" button should select. */
    public void setRecent(boolean b)
    {
        m_recent = b;
    }

    /** Get the "recent" property of this node. */
    public boolean isRecent()
    {
        return m_recent;
    }
        
    /** Returns the first child. 
	@return first child or <code>null</code> if no children.
    */
    public Node getChild() { return getChild(0); }

    /** Returns the most recent child.
	@return most recent child, or first child if no recent one, or
	<code>null</code> if no children.
    */
    public Node getRecentChild() {
        Node cur = m_child;
        if (cur == null) {
            return null;
        }
        for (int i=0; cur != null; i++) {
            if (cur.isRecent()) {
                return cur;
            }
	    cur = cur.getNext();
        }
        return m_child;
    }

    /** Returns the child that contains <code>node</code> in its subtree.
        Currently unused. */
    public Node getChildContainingNode(Node node)
    {
	for (int i=0; i<numChildren(); i++) {
	    Node c = getChild(i);
	    if (c == node) return c;
	}
	for (int i=0; i<numChildren(); i++) {
	    Node c = getChild(i);
	    if (c.getChildContainingNode(node) != null)
		return c;
	}
	return null;
    }

    /** Returns the depth of this node.
     */
    public int getDepth()
    {
        Node cur;
        int depth = 0;
        for (cur = this; ; depth++) {
            Node parent = cur.getParent();
            if (parent == null) break;
            cur = parent;
        }
        return depth;
    }

    /** Determines if the current node is a swap node */
    public boolean isSwap()
    {
        if (this.hasMove()) {
            HexPoint p = m_move.getPoint();
            return p == HexPoint.SWAP_SIDES || p == HexPoint.SWAP_PIECES;
        } else {
            return false;
        }
    }

    //----------------------------------------------------------------------

    /** Adds a property to this node. 
	Node properties are <code>(key, value)</code> pairs of strings.
	These properties will stored if the gametree is saved in SGF format.
	@param key name of the property
	@param value value of the property
    */
    public void setSgfProperty(String key, String value)
    {
	m_property.put(key, value);
    }

    public void unsetSgfProperty(String key)
    {
	m_property.remove(key);
    }

    /** Append the given string to the SGF property */
    public void appendSgfProperty(String key, String toadd)
    {
        String old = m_property.get(key);
        if (old == null) old = "";
        m_property.put(key, old+toadd);
    }

    /** Returns the value of a property. 
	@param key name of property
	@return value of <code>key</code> or <code>null</code> if key is
	not in the property list.
    */                
    public String getSgfProperty(String key)
    {
	return m_property.get(key);
    }

    /** Returns a map of the current set of properties.
	@return Map containing the properties
    */
    public Map<String,String> getProperties()
    {
	return m_property;
    }

    /** Sets the SGF Comment field of this node. */
    public void setComment(String comment) { setSgfProperty("C", comment); }
    
    public String getComment() { return getSgfProperty("C"); }

    public boolean hasCount() 
    {
        return (getSgfProperty("CN") != null);
    }

    public String getCount()
    {
        return getSgfProperty("CN");
    }            

    /** Adds a stone of specified color to the setup list and the sgf
        property string. */
    public void addSetup(HexColor color, HexPoint point)
    {
        m_setup.put(point, color);
    }

    public void removeSetup(HexColor color, HexPoint point)
    {
        m_setup.remove(point);
    }
    
    /** Returns the set of setup stones of color. */
    public Vector<HexPoint> getSetup(HexColor color) 
    {
        Vector<HexPoint> points = new Vector<HexPoint>();
        HexPoint key;
        Iterator i = m_setup.keySet().iterator();
        
        while (i.hasNext()) {
            key = (HexPoint)i.next();
            if (m_setup.get(key) == color) {
                points.add(key);
            }
        }

        return points;
    }

    /** Determine whether the current node has any setup moves */
    public boolean hasSetup()
    {
        return !m_setup.isEmpty();
    }

    /** Determine whether the current node can accept updates to setup
        moves. This happens if the node has no m_move and no children.
        In particular, setup is permitted on the root node (which
        never has an m_move). */
    public boolean canSetup()
    {
        if (this.hasMove()) {
            return false;
        }
        if (this.hasChild()) {
            return false;
        }
        return true;            
    }
    
    
    public boolean hasLabel()
    {
        return !m_label.isEmpty();
    }

    public Vector<String> getLabels()
    {
        return m_label;
    }
    
    public void addLabel(String str)
    {
        m_label.add(str);
    }

    /** Return the default player to move for the current node, i.e.,
        the player who would be moving next if the PL property is not
        set. For normal moves, passes, and swap-pieces, this is the
        opponent of the player who made the move; for swap-sides, it
        is the player who made the move; for the root node, it is
        Black; for resign, forfeit, and setup moves, there is no
        default, i.e., return null */
    public HexColor defaultPlayerToMove()
    {
        if (this.hasMove()) {
            HexPoint p = m_move.getPoint();
            if (p == HexPoint.RESIGN || p == HexPoint.FORFEIT) {
                return null;
            } else if (p == HexPoint.SWAP_SIDES) {
                return m_move.getColor();
            } else {
                return m_move.getColor().otherColor();
            }
        } else if (this.m_parent == null) {
            // root node
            return HexColor.BLACK;
        } else {
            // non-root setup node
            return null;
        }
    }
    
    /** Sets the PL property to the given color. For moves that have a
        default, unset the property if the requested value is the
        default. */
    public void setPlayerToMove(HexColor color)
    {
        if (color == defaultPlayerToMove()) {
            unsetSgfProperty("PL");
        } else {
            setSgfProperty("PL", (color == HexColor.BLACK) ? "B" : "W");
        }
    }

    /** Compute the player to move, using the "PL" property if it is
        set and the standard behavior otherwise. Never return null. */
    public HexColor getPlayerToMove()
    {
        String cstr = getSgfProperty("PL");
        if (cstr != null) {
            if (cstr.equals("B")) {
                return HexColor.BLACK;
            } else if (cstr.equals("W")) {
                return HexColor.WHITE;
            }
        }
        HexColor color = defaultPlayerToMove();
        if (color != null) {
            return color;
        }
        if (this.hasMove()) {
            HexPoint p = m_move.getPoint();
            if (p == HexPoint.RESIGN || p == HexPoint.FORFEIT) {
                if (m_parent != null) {
                    return m_parent.getPlayerToMove();
                }
            }
        }
        return HexColor.BLACK;
    }

    //----------------------------------------------------------------------
    // Debugging output

    /** Print information about self */
    public void printDebug()
    {
        System.out.println("(");
        System.out.println("move: " + m_move);
        System.out.println("setup: " + m_setup);
        System.out.println("label: " + m_label);
        System.out.println("property: " + m_property);
        System.out.println("recent: " + m_recent);
        Node cur = m_child;
        while (cur != null) {
            cur.printDebug();
            cur = cur.getNext();
        }
        System.out.println(")");
    }
    
    //----------------------------------------------------------------------

    private TreeMap<String,String> m_property;

    private Map<HexPoint,HexColor> m_setup;

    private Vector<String> m_label;

    private Move m_move;
    private Node m_parent, m_prev, m_next, m_child;
    private boolean m_recent;
}

//----------------------------------------------------------------------------
