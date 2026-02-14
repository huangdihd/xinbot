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

    @Override
    public AttributedString highlight(final @NonNull LineReader reader, final @NonNull String buffer) {
        final AttributedStringBuilder builder = new AttributedStringBuilder();
        List<String> tokens = tokenize(buffer);
        if (tokens.isEmpty()) return builder.toAttributedString();
        else if (Bot.Instance.getPluginManager().commands().getCommandByLabel(tokens.get(0)) == null) {
            builder.append(tokens.get(0), AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
        }
        else {
            builder.append(tokens.get(0), AttributedStyle.DEFAULT);
        }
        builder.append(" ");
        for (String token : tokens.subList(1, tokens.size() - 1)) {
            builder
                .append(token, AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN))
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
