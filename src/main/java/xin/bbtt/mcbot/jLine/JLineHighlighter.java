package xin.bbtt.mcbot.jLine;

import lombok.NonNull;
import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import xin.bbtt.mcbot.Bot;

import java.util.List;
import java.util.regex.Pattern;

import static xin.bbtt.mcbot.command.CommandManager.tokenize;

public class JLineHighlighter implements Highlighter {
    private static final int[] COLORS = {AttributedStyle.CYAN, AttributedStyle.YELLOW, AttributedStyle.GREEN, AttributedStyle.MAGENTA, AttributedStyle.BLUE};

    @Override
    public AttributedString highlight(final @NonNull LineReader reader, final @NonNull String buffer) {
        final AttributedStringBuilder builder = new AttributedStringBuilder();
        List<String> tokens = tokenize(buffer);
        if (tokens.isEmpty()) return builder.toAttributedString();
        if (tokens.size() == 1 && Bot.Instance.getPluginManager().commands().callComplete(tokens.get(0)).isEmpty()) {
            builder.append(tokens.get(0), AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
        }
        else if (tokens.size() > 1 && Bot.Instance.getPluginManager().commands().getCommandByLabel(tokens.get(0)) == null) {
            builder.append(tokens.get(0), AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
        }
        else {
            builder.append(tokens.get(0), AttributedStyle.DEFAULT);
        }
        builder.append(" ");
        for (int i = 1;i < tokens.size();i++) {
            builder
                .append(tokens.get(i), AttributedStyle.DEFAULT.foreground(COLORS[i % COLORS.length]))
                .append(" ");
        }
        return builder.toAttributedString();
        }

    @Override
    public void setErrorPattern(Pattern pattern) {

    }

    @Override
    public void setErrorIndex(int i) {

    }
}
