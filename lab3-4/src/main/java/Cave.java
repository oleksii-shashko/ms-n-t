import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class Cave {

    private int caveX; // starts left -> right
    private int caveY; // starts bottom ^ up

    private AgentPosition start = new AgentPosition(1, 1, AgentPosition.Orientation.FACING_NORTH);

    private Cell wumpus;
    private Cell gold;

    private Set<Cell> pits = new LinkedHashSet<>();
    private Set<Cell> allowedCells;

    public Cave() {
        this(4,4);
    }

    public Cave(int caveX, int caveY) {
        if (caveX < 1)
            throw new IllegalArgumentException("Cave must have x dimension >= 1");
        if (caveY < 1)
            throw new IllegalArgumentException("Case must have y dimension >= 1");

        this.caveX = caveX;
        this.caveY = caveY;

        allowedCells = getAllRooms();
    }

    public Cave(int caveX, int caveY, String config) {
        this(caveX, caveY);

        if (config.length() != 2 * caveX * caveY)
            throw new IllegalStateException("Wrong configuration length.");

        for (int i = 0; i < config.length(); i += 2) {
            char cellChar = config.charAt(i);

            Cell cell = new Cell(i / 2 % caveX + 1, caveY - i / 2 / caveX);

            switch (cellChar) {
                case 'S': start = new AgentPosition(cell.getX(), cell.getY(), AgentPosition.Orientation.FACING_NORTH); break;
                case 'W': wumpus = cell; break;
                case 'G': gold = cell; break;
                case 'P': pits.add(cell); break;
            }
        }
    }

    public Cave setAllowed(Set<Cell> allowedCells) {
        this.allowedCells.clear();
        this.allowedCells.addAll(allowedCells);

        return this;
    }

    public void setWumpus(Cell cell) {
        wumpus = cell;
    }

    public void setGold(Cell cell) {
        gold = cell;
    }

    public void setPit(Cell cell, boolean b) {
        if (!b)
            pits.remove(cell);
        else if (!cell.equals(start.getRoom()) && !cell.equals(gold))
            pits.add(cell);
    }

    public int getCaveX() {
        return caveX;
    }

    public int getCaveY() {
        return caveY;
    }

    public AgentPosition getStart() {
        return start;
    }

    public Cell getWumpus() {
        return wumpus;
    }

    public Cell getGold() {
        return gold;
    }

    public boolean isPit(Cell cell) {
        return pits.contains(cell);
    }

    public AgentPosition moveForward(AgentPosition position) {
        int x = position.getX();
        int y = position.getY();

        switch (position.getOrientation()) {
            case FACING_NORTH: y++; break;
            case FACING_SOUTH: y--; break;
            case FACING_EAST: x++; break;
            case FACING_WEST: x--; break;
        }

        start = allowedCells.contains(new Cell(x, y)) ?
                new AgentPosition(x, y, position.getOrientation()) :
                position;

        return start;
    }

    public AgentPosition turnLeft(AgentPosition position) {
        AgentPosition.Orientation orientation = null;

        switch (position.getOrientation()) {
            case FACING_NORTH: orientation = AgentPosition.Orientation.FACING_WEST; break;
            case FACING_SOUTH: orientation = AgentPosition.Orientation.FACING_EAST; break;
            case FACING_EAST: orientation = AgentPosition.Orientation.FACING_NORTH; break;
            case FACING_WEST: orientation = AgentPosition.Orientation.FACING_SOUTH; break;
        }

        start = new AgentPosition(position.getX(), position.getY(), orientation);

        return start;
    }

    public AgentPosition turnRight(AgentPosition position) {
        AgentPosition.Orientation orientation = null;

        switch (position.getOrientation()) {
            case FACING_NORTH: orientation = AgentPosition.Orientation.FACING_EAST; break;
            case FACING_SOUTH: orientation = AgentPosition.Orientation.FACING_WEST; break;
            case FACING_EAST: orientation = AgentPosition.Orientation.FACING_SOUTH; break;
            case FACING_WEST: orientation = AgentPosition.Orientation.FACING_NORTH; break;
        }

        start = new AgentPosition(position.getX(), position.getY(), orientation);

        return start;
    }

    public Set<Cell> getAllRooms() {
        Set<Cell> allowedCells = new HashSet<>();

        for (int x = 1; x <= caveX; x++)
            for (int y = 1; y <= caveY; y++)
                allowedCells.add(new Cell(x, y));

        return allowedCells;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        for (int y = caveY; y >= 1; y--) {
            for (int x = 1; x <= caveX; x++) {
                Cell cell = new Cell(x, y);
                String txt = "";

                if (cell.equals(start.getRoom())) txt += "S ";
                if (cell.equals(gold)) txt += "G ";
                if (cell.equals(wumpus)) txt += "W ";
                if (isPit(cell)) txt += "P ";
                if (txt.isEmpty()) txt = "_ ";

                builder.append(txt);
            }
            builder.append("\n");
        }
        return builder.toString();
    }
}
