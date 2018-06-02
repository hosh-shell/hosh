grammar Hosh;

program
    : EOF
    | stmt+ EOF
    ;

stmt
    : ID ID* NEWLINE
    ;

 ID
    : ([a-zA-Z0-9-] | '.') +
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