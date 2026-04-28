class Task {
    String name;
    String priority;
    String deadline;

    public Task(String name, String priority, String deadline) {
        this.name = name;
        this.priority = priority;
        this.deadline = deadline;
    }

    public String toString() {
        return "name=" + name +
               ";priority=" + priority +
               ";deadline=" + deadline;
    }

    public static Task fromString(String taskData) {

        String[] parts = taskData.split(";");

        String name = null;
        String priority = null;
        String deadline = null;

        for (String part : parts) {
            String[] keyValue = part.split("=");

            if (keyValue.length != 2) continue;

            String key = keyValue[0].trim();
            String value = keyValue[1].trim();

            switch (key) {
                case "name":
                    name = value;
                    break;

                case "priority":
                    priority = value;
                    break;

                case "deadline":
                    deadline = value;
                    break;
            }
        }

        return new Task(name, priority, deadline);
    }
}
