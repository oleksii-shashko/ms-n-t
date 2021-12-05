public class Cell {
    private int x = 1;
    private int y = 1;

    public Cell(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public String toString() {
        return "[" + x + "," + y + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Cell) {
            Cell other = (Cell) obj;
            return x == other.x && y == other.y;
        }
        else return false;
    }

    @Override
    public int hashCode() {
        int result = 13;
        result = 14 * result + getX();
        result = 88 * result + getY();
        return result;
    }
}