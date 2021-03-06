package org.quantumbadger.redreader.reddit.prepared.markdown;

import android.graphics.Typeface;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import org.holoeverywhere.app.Activity;
import org.quantumbadger.redreader.common.LinkHandler;

import java.util.ArrayList;
import java.util.List;

// TODO spoilers
// TODO number links
public final class MarkdownParagraph {

	final CharArrSubstring raw;
	final MarkdownParagraph parent;
	final MarkdownParser.MarkdownParagraphType type;
	final int[] tokens;
	final int level;

	final Spanned spanned;
	final List<Link> links;

	public class Link {
		final String title;
		final String subtitle;
		private final String url;

		public Link(String title, String subtitle, String url) {
			this.title = title;
			this.subtitle = subtitle;
			this.url = url;
		}

		public void onClicked(Activity activity) {
			LinkHandler.onLinkClicked(activity, url, false);
		}
	}

	public MarkdownParagraph(CharArrSubstring raw, MarkdownParagraph parent, MarkdownParser.MarkdownParagraphType type,
							 int[] tokens, int level) {
		this.raw = raw;
		this.parent = parent;
		this.type = type;
		this.tokens = tokens;
		this.level = level;

		links = new ArrayList<Link>();
		spanned = internalGenerateSpanned();
	}

	// TODO superscript
	private Spanned internalGenerateSpanned() {

		if(type == MarkdownParser.MarkdownParagraphType.CODE || type == MarkdownParser.MarkdownParagraphType.HLINE) {
			return null;
		}

		final SpannableStringBuilder builder = new SpannableStringBuilder();

		// TODO double check these start at builder.length(), not i
		// TODO bold/italic using underscores, taking into account special cases (e.g. a_b_c vs ._b_.)
		int boldStart = -1, italicStart = -1, strikeStart = -1, linkStart = -1;

		for(int i = 0; i < tokens.length; i++) {

			final int token = tokens[i];

			switch(token) {

				case MarkdownTokenizer.TOKEN_ASTERISK:

					if(italicStart < 0) {
						italicStart = builder.length();
					} else {
						builder.setSpan(new StyleSpan(Typeface.ITALIC), italicStart, builder.length(),
								Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
						italicStart = -1;
					}

					break;

				case MarkdownTokenizer.TOKEN_ASTERISK_DOUBLE:

					if(boldStart < 0) {
						boldStart = builder.length();
					} else {
						builder.setSpan(new StyleSpan(Typeface.BOLD), boldStart, builder.length(),
								Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
						boldStart = -1;
					}

					break;


				case MarkdownTokenizer.TOKEN_TILDE_DOUBLE:

					if(strikeStart == -1) {
						strikeStart = builder.length();

					} else {
						builder.setSpan(new StrikethroughSpan(), strikeStart, builder.length(),
								Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
						strikeStart = -1;
					}

					break;

				case MarkdownTokenizer.TOKEN_GRAVE:

					final int codeStart = builder.length();

					while(tokens[++i] != MarkdownTokenizer.TOKEN_GRAVE) {
						builder.append((char)tokens[i]);
					}

					builder.setSpan(new TypefaceSpan("monospace"), codeStart, builder.length(),
							Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

					break;

				case MarkdownTokenizer.TOKEN_BRACKET_SQUARE_OPEN:
					linkStart = builder.length();
					break;

				case MarkdownTokenizer.TOKEN_BRACKET_SQUARE_CLOSE:

					final int urlStart = indexOf(tokens, MarkdownTokenizer.TOKEN_PAREN_OPEN, i + 1);
					final int urlEnd = indexOf(tokens, MarkdownTokenizer.TOKEN_PAREN_CLOSE, urlStart + 1);

					final StringBuilder urlBuilder = new StringBuilder(urlEnd - urlStart);

					for(int j = urlStart + 1; j < urlEnd; j++) {
						urlBuilder.append((char)tokens[j]);
					}

					final String linkText = String.valueOf(builder.subSequence(linkStart, builder.length()));
					final String url = urlBuilder.toString();

					if(url.startsWith("/spoiler")) {

						builder.delete(linkStart, builder.length());
						builder.append("[Spoiler]");

						final Uri.Builder spoilerUriBuilder = Uri.parse("rr://msg/").buildUpon();
						spoilerUriBuilder.appendQueryParameter("title", "Spoiler");
						spoilerUriBuilder.appendQueryParameter("message", linkText);

						links.add(new Link("Spoiler", null, spoilerUriBuilder.toString()));

					} else if(url.length() > 3 && url.charAt(2) == ' '
							&& (url.charAt(0) == '#' || url.charAt(0) == '/')) {

						final String subtitle;
						switch(url.charAt(1)) {
							case 'b':
								subtitle = "Spoiler: Book";
								break;
							case 'g':
								subtitle = "Spoiler: Speculation";
								break;
							case 's':
							default:
								subtitle = "Spoiler";
								break;
						}

						final Uri.Builder spoilerUriBuilder = Uri.parse("rr://msg/").buildUpon();
						spoilerUriBuilder.appendQueryParameter("title", subtitle);
						spoilerUriBuilder.appendQueryParameter("message", url.substring(3));

						links.add(new Link(linkText, subtitle, spoilerUriBuilder.toString()));

					} else {
						links.add(new Link(linkText, url, url));
					}

					// TODO
					//builder.insert(linkStart, "[NUMBER HERE]");

					builder.setSpan(new URLSpan(url), linkStart, builder.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

					i = urlEnd;

					break;

				case MarkdownTokenizer.TOKEN_CARET:
					// TODO
					builder.append('^');
					break;

				default:
					builder.append((char)token);
					break;
			}
		}

		return builder;
	}

	private static int indexOf(final int[] haystack, final int needle, final int startPos) {
		for(int i = startPos; i < haystack.length; i++) {
			if(haystack[i] == needle) return i;
		}
		return -1;
	}
}
