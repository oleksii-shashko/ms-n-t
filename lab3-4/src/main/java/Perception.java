public class Perception {

    private boolean stench;
    private boolean breeze;
    private boolean glitter;
    private boolean scream;

    public Perception setStench() {
        stench = true;
        return this;
    }

    public Perception setBreeze() {
        breeze = true;
        return this;
    }

    public Perception setGlitter() {
        glitter = true;
        return this;
    }

    public Perception setScream() {
        scream = true;
        return this;
    }

    public boolean isStench() {
        return stench;
    }

    public boolean isBreeze() {
        return breeze;
    }

    public boolean isGlitter() {
        return glitter;
    }

    public boolean isScream() {
        return scream;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        if (stench) result.append("There is Stench. ");
        if (breeze) result.append("There is Breeze. ");
        if (glitter) result.append("There is Glitter. ");
        if (scream) result.append("There is Scream. ");

        return result.toString();
    }
}