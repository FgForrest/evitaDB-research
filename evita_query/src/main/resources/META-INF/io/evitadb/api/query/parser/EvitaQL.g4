/**
 * ANTLRv4 grammar for Evita Query Language (EvitaQL) - parser and lexer
 */
grammar EvitaQL;

@header {
package io.evitadb.api.query.parser.grammar;
}

/**
 * Root rule of EvitaQL
 */
root
    : query EOF
    | constraint EOF
    | literal EOF
    ;


/**
 * Whole query with constraints
 */
query : 'query' args = constraintListArgs ;


/**
 * Constraints rules.
 */

constraint
    : headConstraint
    | filterConstraint
    | orderConstraint
    | requireConstraint
    ;

headConstraint
    : 'entities'                    args = entityTypeArgs                                   # entitiesConstraint
    ;

filterConstraint
    : 'filterBy'                    args = filterConstraintContainerArgs                    # filterByConstraint
    | 'and'                         args = filterConstraintContainerArgs                    # andConstraint
    | 'or'                          args = filterConstraintContainerArgs                    # orConstraint
    | 'not'                         args = filterConstraintContainerArgs                    # notConstraint
    | 'userFilter'                  args = filterConstraintContainerArgs                    # userFilterConstraint
    | 'equals'                      args = nameWithValueArgs                                # equalsConstraint
    | 'greaterThan'                 args = nameWithValueArgs                                # greaterThanConstraint
    | 'greaterThanEquals'           args = nameWithValueArgs                                # greaterThanEqualsConstraint
    | 'lessThan'                    args = nameWithValueArgs                                # lessThanConstraint
    | 'lessThanEquals'              args = nameWithValueArgs                                # lessThanEqualsConstraint
    | 'between'                     args = nameWithBetweenValuesArgs                        # betweenConstraint
    | 'inSet'                       args = nameWithValueListArgs                            # inSetConstraint
    | 'contains'                    args = nameWithValueArgs                                # containsConstraint
    | 'startsWith'                  args = nameWithValueArgs                                # startsWithConstraint
    | 'endsWith'                    args = nameWithValueArgs                                # endsWithConstraint
    | 'isTrue'                      args = nameArgs                                         # isTrueConstraint
    | 'isFalse'                     args = nameArgs                                         # isFalseConstraint
    | 'isNull'                      args = nameArgs                                         # isNullConstraint
    | 'isNotNull'                   args = nameArgs                                         # isNotNullConstraint
    | 'inRange'                     args = nameWithValueArgs                                # inRangeConstraint
    | 'primaryKey'                  args = valueListArgs                                    # primaryKeyConstraint
    | 'language'                    args = valueArgs                                        # languageConstraint
    | 'priceInCurrency'             args = valueArgs                                        # priceInCurrencyConstraint
    | 'priceInPriceLists'           args = valueListArgs                                    # priceInPriceListsConstraints
    | 'priceValidIn'                args = valueArgs                                        # priceValidInConstraint
    | 'priceBetween'                args = betweenValuesArgs                                # priceBetweenConstraint
    | 'facet'                       args = entityTypeWithValueListArgs                      # facetConstraint
    | 'referenceHavingAttribute'    args = entityTypeWithFilterConstraintArgs               # referenceHavingAttributeConstraint
    | 'withinHierarchy'             args = withinHierarchyConstraintArgs                    # withinHierarchyConstraint
    | 'withinRootHierarchy'         args = withinRootHierarchyConstraintArgs                # withinRootHierarchyConstraint
    | 'directRelation'              emptyArgs                                               # directRelationConstraint
    | 'excludingRoot'               emptyArgs                                               # excludingRootConstraint
    | 'excluding'                   args = valueListArgs                                    # excludingConstraint
    ;

orderConstraint
    : 'orderBy'                     args = orderConstraintContainerArgs                     # orderByConstraint
    | 'ascending'                   args = nameArgs                                         # ascendingConstraint
    | 'descending'                  args = nameArgs                                         # descendingConstraint
    | 'priceAscending'              emptyArgs                                               # priceAscendingConstraint
    | 'priceDescending'             emptyArgs                                               # priceDescendingConstraint
    | 'random'                      emptyArgs                                               # randomConstraint
    | 'referenceAttribute'          args = entityTypeWithOrderConstraintListArgs            # referenceAttributeConstraint
    ;

requireConstraint
    : 'require'                     args = requireConstraintContainerArgs                   # requireContainerConstraint
    | 'page'                        args = pageConstraintArgs                               # pageConstraint
    | 'strip'                       args = stripConstraintArgs                              # stripConstraint
    | 'entityBody'                  emptyArgs                                               # entityBodyConstraint
    | 'attributes'                  emptyArgs                                               # attributesConstraint
    | 'prices'                      (emptyArgs | args = valueArgs)                          # pricesConstraint
    | 'associatedData'              (emptyArgs | args = nameListArgs)                       # associatedDataConstraint
    | 'references'                  (emptyArgs | args = referencedTypesArgs)                # referencesConstraint
    | 'useOfPrice'                  args = valueArgs                                        # useOfPriceConstraint
    | 'dataInLanguage'              (emptyArgs | args = valueListArgs)                      # dataInLanguageConstraint
    | 'parents'                     (emptyArgs | args = requireConstraintContainerArgs)     # parentsConstraint
    | 'parentsOfType'               args = parentsOfTypeConstraintArgs                      # parentsOfTypeConstraint
    | 'facetSummary'                (emptyArgs | args = valueArgs)                          # facetSummaryConstraint
    | 'facetGroupsConjunction'      args = entityTypeWithValueListArgs                      # facetGroupsConjunctionConstraint
    | 'facetGroupsDisjunction'      args = entityTypeWithValueListArgs                      # facetGroupsDisjunctionConstraint
    | 'facetGroupsNegation'          args = entityTypeWithValueListArgs                      # facetGroupsNegationConstraint
    | 'attributeHistogram'          args = valueWithNameListArgs                            # attributeHistogramConstraint
    | 'priceHistogram'              args = valueArgs                                        # priceHistogramConstraint
    | 'hierarchyStatistics'         args = entityTypeWithRequireConstraintListArgs          # hierarchyStatisticsConstraint
    ;


/**
 * Arguments syntax rules for query and constraints.
 * Used for better reusability and clearer generated contexts' structure ("args" label).
 */

constraintListArgs :                        ARGS_OPENING constraints += constraint (ARGS_DELIMITER constraints += constraint)* ARGS_CLOSING ;

emptyArgs :                                 ARGS_OPENING ARGS_CLOSING ;

filterConstraintContainerArgs :             ARGS_OPENING constraints += filterConstraint (ARGS_DELIMITER constraints += filterConstraint)* ARGS_CLOSING ;

orderConstraintContainerArgs :              ARGS_OPENING constraints += orderConstraint (ARGS_DELIMITER constraints += orderConstraint)* ARGS_CLOSING ;

requireConstraintContainerArgs :            ARGS_OPENING constraints += requireConstraint (ARGS_DELIMITER constraints += requireConstraint)* ARGS_CLOSING ;

nameArgs :                                  ARGS_OPENING name = identifier ARGS_CLOSING ;

nameWithValueArgs :                         ARGS_OPENING name = identifier ARGS_DELIMITER value = literal ARGS_CLOSING ;

nameWithValueListArgs :                     ARGS_OPENING name = identifier (ARGS_DELIMITER values += literal)+ ARGS_CLOSING ;

nameWithBetweenValuesArgs :                 ARGS_OPENING name = identifier ARGS_DELIMITER valueFrom = literal ARGS_DELIMITER valueTo = literal ARGS_CLOSING ;

valueArgs :                                 ARGS_OPENING value = literal ARGS_CLOSING ;

valueListArgs :                             ARGS_OPENING values += literal (ARGS_DELIMITER values += literal)* ARGS_CLOSING ;

betweenValuesArgs :                         ARGS_OPENING valueFrom = literal ARGS_DELIMITER valueTo = literal ARGS_CLOSING ;

nameListArgs :                              ARGS_OPENING names += identifier (ARGS_DELIMITER names += identifier)* ARGS_CLOSING ;

valueWithNameListArgs :                     ARGS_OPENING value = literal (ARGS_DELIMITER names += identifier)+ ARGS_CLOSING ;

referencedTypesArgs :                       ARGS_OPENING referencedTypes += literal (ARGS_DELIMITER referencedTypes += literal)* ARGS_CLOSING ;

entityTypeArgs :                            ARGS_OPENING entityType = literal ARGS_CLOSING ;

entityTypeListArgs :                        ARGS_OPENING entityTypes += literal (ARGS_DELIMITER entityTypes += literal)* ARGS_CLOSING ;

entityTypeWithValueListArgs :               ARGS_OPENING entityType = literal (ARGS_DELIMITER values += literal)+ ARGS_CLOSING ;

entityTypeWithFilterConstraintArgs :        ARGS_OPENING entityType = literal ARGS_DELIMITER filterConstraint ARGS_CLOSING ;

entityTypeWithOrderConstraintListArgs :     ARGS_OPENING entityType = literal (ARGS_DELIMITER constrains += orderConstraint)+ ARGS_CLOSING ;

entityTypeWithRequireConstraintListArgs:    ARGS_OPENING entityType = literal (ARGS_DELIMITER constrains += requireConstraint)* ARGS_CLOSING ;

withinHierarchyConstraintArgs :             ARGS_OPENING (entityType = literal ARGS_DELIMITER)? primaryKey = literal (ARGS_DELIMITER constrains += filterConstraint)* ARGS_CLOSING ;

withinRootHierarchyConstraintArgs :         ARGS_OPENING (entityType = literal | ((entityType = literal ARGS_DELIMITER)? (constrains += filterConstraint (ARGS_DELIMITER constrains += filterConstraint)*)?)) ARGS_CLOSING ;

pageConstraintArgs :                        ARGS_OPENING pageNumber = literal ARGS_DELIMITER pageSize = literal ARGS_CLOSING ;

stripConstraintArgs :                       ARGS_OPENING offset = literal ARGS_DELIMITER limit = literal ARGS_CLOSING ;

parentsOfTypeConstraintArgs :               ARGS_OPENING entityTypes += literal (ARGS_DELIMITER entityTypes += literal)* (ARGS_DELIMITER requireConstraints += requireConstraint)* ARGS_CLOSING ;


/**
 * Identifier rule representing names of attributes, associated data etc. inside of Evita constraints
 */
identifier : STRING ;


/**
 * Literal rule representing any value supported by Evita data types
 */
literal
    : STRING                                                                                    # stringLiteral
    | INT                                                                                       # intLiteral
    | FLOAT                                                                                     # floatLiteral
    | BOOLEAN                                                                                   # booleanLiteral
    | DATE                                                                                      # dateLiteral
    | TIME                                                                                      # timeLiteral
    | DATE_TIME                                                                                 # dateTimeLiteral
    | ZONED_DATE_TIME                                                                           # zonedDateTimeLiteral
    | NUMBER_RANGE                                                                              # numberRangeLiteral
    | DATE_TIME_RANGE                                                                           # dateTimeRangeLiteral
    | ENUM                                                                                      # enumLiteral
    | LOCALE                                                                                    # localeLiteral
    | MULTIPLE_OPENING values += literal (ARGS_DELIMITER values += literal)+ MULTIPLE_CLOSING   # multipleLiteral
    ;


/**
 * Literal value tokens
 */

STRING : '\'' .*? '\'' ;

INT : [0-9]+ ;

FLOAT : [0-9]* '.' [0-9]+ ;

BOOLEAN
    : 'false'
    | 'true'
    ;

DATE : [0-9][0-9][0-9][0-9] '-' [0-9][0-9] '-' [0-9][0-9] ;

TIME : [0-9][0-9] ':' [0-9][0-9] ':' [0-9][0-9] ;

DATE_TIME : DATE 'T' TIME ;

ZONED_DATE_TIME : DATE_TIME ('+'|'-') [0-9][0-9] ':' [0-9][0-9] '[' [a-zA-Z]+ '/' [a-zA-Z]+ ']' ;

NUMBER_RANGE : '[' ( INT | FLOAT )? ',' ( INT | FLOAT )? ']' ;

DATE_TIME_RANGE : '[' ZONED_DATE_TIME? ',' ZONED_DATE_TIME? ']' ;

ENUM : [A-Z]+ ('_' [A-Z]+)* ;

LOCALE : '`' [a-z]+ ('_' [A-Z]+)?  '`' ;


/**
 * General delimiter tokens
 */

ARGS_OPENING : '(' ;

ARGS_CLOSING : ')' ;

ARGS_DELIMITER : ',' ;

MULTIPLE_OPENING : '{' ;

MULTIPLE_CLOSING : '}' ;


/**
 * Miscellaneous tokens
 */

WHITESPACE : [ \r\t\n]+ -> skip ;

UNEXPECTED_CHAR : . ;
