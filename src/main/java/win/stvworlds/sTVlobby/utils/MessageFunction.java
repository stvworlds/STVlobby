package win.stvworlds.sTVlobby.utils;

import java.util.Arrays;

public interface MessageFunction {
    enum Info {
        CORE("core", "⛏", "Основні системні команди STVlobby"),
        LINK("link", "✦", "Прив'язування майнкрафту до акаунту");


        private final String name;
        private final String symbol;
        private final String description;

        Info(String name, String symbol, String description) {
            this.name = name;
            this.symbol = symbol;
            this.description = description;
        }

        public String getSymbol() {
            return symbol;
        }

        public String getDescription() {
            return description;
        }

        public static Info fromName(String name) {
            if (name == null || name.isBlank()) {
                return null;
            }

            return Arrays.stream(values())
                    .filter(info -> info.name.equalsIgnoreCase(name))
                    .findFirst()
                    .orElse(null);
        }
    }
}
