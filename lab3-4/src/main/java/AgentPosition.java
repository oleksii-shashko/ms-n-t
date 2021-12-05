public class AgentPosition {

    public enum Orientation {
        FACING_NORTH("FacingNorth"),
        FACING_SOUTH("FacingSouth"),
        FACING_EAST("FacingEast"),
        FACING_WEST("FacingWest");

        public String getSymbol() {
            return symbol;
        }

        private final String symbol;

        Orientation(String sym) {
            symbol = sym;
        }
    }

    private Cell cell;
    private Orientation orientation;

    public AgentPosition(int x, int y, Orientation orientation) {
        this(new Cell(x, y), orientation);
    }

    public AgentPosition(Cell cell, Orientation orientation) {
        this.cell = cell;
        this.orientation = orientation;
    }

    public Cell getRoom() {
        return cell;
    }

    public int getX() {
        return cell.getX();
    }

    public int getY() {
        return cell.getY();
    }

    public Orientation getOrientation() {
        return orientation;
    }

    @Override
    public String toString() {
        return cell.toString() + "->" + orientation.getSymbol();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AgentPosition){
            AgentPosition other = (AgentPosition) obj;

            return (getX() == other.getX()) &&
                    (getY() == other.getY()) &&
                    (orientation == other.getOrientation());
        }
        else
            return false;
    }

    @Override
    public int hashCode() {
        int result = 13;
        result = 14 * result + getX();
        result = 88 * result + getY();
        return result;
    }
}
