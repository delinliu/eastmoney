package parser;

import java.util.Set;

public interface ListParserInterface {

	Set<String> parseList(String pageContent) throws ParserException;
}
