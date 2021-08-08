lexer grammar HoshLexer;

PIPE
	: '|'
	;

OPEN_CURLY
	: '{'
	;

CLOSE_CURLY
	: '}'
	;

ARROW
	: '->'
	;

SEMICOLON
	: ';'
	;

NEWLINE
	: '\n'
	| '\r\n'
	;

OPEN_SQUOTE
	: '\'' -> pushMode(LITERAL_STRING)
	;

OPEN_DQUOTE
	: '"' -> pushMode(STRING)
	;

// ID will be checked by the compiler on case by case basic: sometimes it is a variable name, sometimes it is a literal
ID
	: I+
	;

VARIABLE
	: '$' '{' ID '}'
	;

VARIABLE_OR_FALLBACK
	: '$' '{' ID '!' ID '}'
	;

// try to simplify by using modes (i.e. LITERAL_STRING): "I" should be able to define paths for all OS (so that's why we have / \\)
fragment I : LOWER | UPPER | DIGIT | ':' | '_' | '-' | '.' | '/' | '\\' | '~' | '+' | '*' | '=' | '?' | '<' | '>' | '(' | ')' | ',' ;

fragment LOWER: 'a'..'z';

fragment UPPER: 'A'..'Z';

fragment DIGIT: '0'..'9';

COMMENT
	:  '#' ~('\r' | '\n')* -> skip
	;

WS
	: [ \t]+ -> skip
	;

mode LITERAL_STRING;

CLOSE_SQUOTE: '\'' -> popMode;

SQUOTE_TEXT
   	: ~[']+
	;

mode STRING;

CLOSE_DQUOTE: '"' -> popMode;

DQUOTE_TEXT
	: ~('$' | '"')+
	;

DQUOTE_VARIABLE
	: '${' ID CLOSE_CURLY
	;

DQUOTE_VARIABLE_OR_FALLBACK
	: '${' ID '!' ID CLOSE_CURLY
	;

