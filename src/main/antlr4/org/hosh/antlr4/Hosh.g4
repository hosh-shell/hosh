grammar Hosh;

program
    : stmt*
    ;

stmt
    : ID ID* NEWLINE
    ;

 ID
    : ([a-zA-Z0-9] | '-' | '.' | '/') +
    ;

NEWLINE
    : '\n'
    ;

WS
    : [ \t]+ -> skip
    ;

LINE_COMMENT
    : '#' .*? '\r'? '\n' -> skip
    ;