// Generated from EvitaQL.g4 by ANTLR 4.9.2

package io.evitadb.api.query.parser.grammar;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class EvitaQLParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.9.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, T__14=15, T__15=16, T__16=17, 
		T__17=18, T__18=19, T__19=20, T__20=21, T__21=22, T__22=23, T__23=24, 
		T__24=25, T__25=26, T__26=27, T__27=28, T__28=29, T__29=30, T__30=31, 
		T__31=32, T__32=33, T__33=34, T__34=35, T__35=36, T__36=37, T__37=38, 
		T__38=39, T__39=40, T__40=41, T__41=42, T__42=43, T__43=44, T__44=45, 
		T__45=46, T__46=47, T__47=48, T__48=49, T__49=50, T__50=51, T__51=52, 
		T__52=53, T__53=54, T__54=55, T__55=56, T__56=57, T__57=58, T__58=59, 
		T__59=60, T__60=61, STRING=62, INT=63, FLOAT=64, BOOLEAN=65, DATE=66, 
		TIME=67, DATE_TIME=68, ZONED_DATE_TIME=69, NUMBER_RANGE=70, DATE_TIME_RANGE=71, 
		ENUM=72, LOCALE=73, ARGS_OPENING=74, ARGS_CLOSING=75, ARGS_DELIMITER=76, 
		MULTIPLE_OPENING=77, MULTIPLE_CLOSING=78, WHITESPACE=79, UNEXPECTED_CHAR=80;
	public static final int
		RULE_root = 0, RULE_query = 1, RULE_constraint = 2, RULE_headConstraint = 3, 
		RULE_filterConstraint = 4, RULE_orderConstraint = 5, RULE_requireConstraint = 6, 
		RULE_constraintListArgs = 7, RULE_emptyArgs = 8, RULE_filterConstraintContainerArgs = 9, 
		RULE_orderConstraintContainerArgs = 10, RULE_requireConstraintContainerArgs = 11, 
		RULE_nameArgs = 12, RULE_nameWithValueArgs = 13, RULE_nameWithValueListArgs = 14, 
		RULE_nameWithBetweenValuesArgs = 15, RULE_valueArgs = 16, RULE_valueListArgs = 17, 
		RULE_betweenValuesArgs = 18, RULE_nameListArgs = 19, RULE_valueWithNameListArgs = 20, 
		RULE_referencedTypesArgs = 21, RULE_entityTypeArgs = 22, RULE_entityTypeListArgs = 23, 
		RULE_entityTypeWithValueListArgs = 24, RULE_entityTypeWithFilterConstraintArgs = 25, 
		RULE_entityTypeWithOrderConstraintListArgs = 26, RULE_entityTypeWithRequireConstraintListArgs = 27, 
		RULE_withinHierarchyConstraintArgs = 28, RULE_withinRootHierarchyConstraintArgs = 29, 
		RULE_pageConstraintArgs = 30, RULE_stripConstraintArgs = 31, RULE_parentsOfTypeConstraintArgs = 32, 
		RULE_identifier = 33, RULE_literal = 34;
	private static String[] makeRuleNames() {
		return new String[] {
			"root", "query", "constraint", "headConstraint", "filterConstraint", 
			"orderConstraint", "requireConstraint", "constraintListArgs", "emptyArgs", 
			"filterConstraintContainerArgs", "orderConstraintContainerArgs", "requireConstraintContainerArgs", 
			"nameArgs", "nameWithValueArgs", "nameWithValueListArgs", "nameWithBetweenValuesArgs", 
			"valueArgs", "valueListArgs", "betweenValuesArgs", "nameListArgs", "valueWithNameListArgs", 
			"referencedTypesArgs", "entityTypeArgs", "entityTypeListArgs", "entityTypeWithValueListArgs", 
			"entityTypeWithFilterConstraintArgs", "entityTypeWithOrderConstraintListArgs", 
			"entityTypeWithRequireConstraintListArgs", "withinHierarchyConstraintArgs", 
			"withinRootHierarchyConstraintArgs", "pageConstraintArgs", "stripConstraintArgs", 
			"parentsOfTypeConstraintArgs", "identifier", "literal"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'query'", "'entities'", "'filterBy'", "'and'", "'or'", "'not'", 
			"'userFilter'", "'equals'", "'greaterThan'", "'greaterThanEquals'", "'lessThan'", 
			"'lessThanEquals'", "'between'", "'inSet'", "'contains'", "'startsWith'", 
			"'endsWith'", "'isTrue'", "'isFalse'", "'isNull'", "'isNotNull'", "'inRange'", 
			"'primaryKey'", "'language'", "'priceInCurrency'", "'priceInPriceLists'", 
			"'priceValidIn'", "'priceBetween'", "'facet'", "'referenceHavingAttribute'", 
			"'withinHierarchy'", "'withinRootHierarchy'", "'directRelation'", "'excludingRoot'", 
			"'excluding'", "'orderBy'", "'ascending'", "'descending'", "'priceAscending'", 
			"'priceDescending'", "'random'", "'referenceAttribute'", "'require'", 
			"'page'", "'strip'", "'entityBody'", "'attributes'", "'prices'", "'associatedData'", 
			"'references'", "'useOfPrice'", "'dataInLanguage'", "'parents'", "'parentsOfType'", 
			"'facetSummary'", "'facetGroupsConjunction'", "'facetGroupsDisjunction'", 
			"'facetGroupsNegation'", "'attributeHistogram'", "'priceHistogram'", 
			"'hierarchyStatistics'", null, null, null, null, null, null, null, null, 
			null, null, null, null, "'('", "')'", "','", "'{'", "'}'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, "STRING", "INT", "FLOAT", "BOOLEAN", "DATE", "TIME", "DATE_TIME", 
			"ZONED_DATE_TIME", "NUMBER_RANGE", "DATE_TIME_RANGE", "ENUM", "LOCALE", 
			"ARGS_OPENING", "ARGS_CLOSING", "ARGS_DELIMITER", "MULTIPLE_OPENING", 
			"MULTIPLE_CLOSING", "WHITESPACE", "UNEXPECTED_CHAR"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "EvitaQL.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public EvitaQLParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	public static class RootContext extends ParserRuleContext {
		public QueryContext query() {
			return getRuleContext(QueryContext.class,0);
		}
		public TerminalNode EOF() { return getToken(EvitaQLParser.EOF, 0); }
		public ConstraintContext constraint() {
			return getRuleContext(ConstraintContext.class,0);
		}
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public RootContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_root; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterRoot(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitRoot(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitRoot(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RootContext root() throws RecognitionException {
		RootContext _localctx = new RootContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_root);
		try {
			setState(79);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__0:
				enterOuterAlt(_localctx, 1);
				{
				setState(70);
				query();
				setState(71);
				match(EOF);
				}
				break;
			case T__1:
			case T__2:
			case T__3:
			case T__4:
			case T__5:
			case T__6:
			case T__7:
			case T__8:
			case T__9:
			case T__10:
			case T__11:
			case T__12:
			case T__13:
			case T__14:
			case T__15:
			case T__16:
			case T__17:
			case T__18:
			case T__19:
			case T__20:
			case T__21:
			case T__22:
			case T__23:
			case T__24:
			case T__25:
			case T__26:
			case T__27:
			case T__28:
			case T__29:
			case T__30:
			case T__31:
			case T__32:
			case T__33:
			case T__34:
			case T__35:
			case T__36:
			case T__37:
			case T__38:
			case T__39:
			case T__40:
			case T__41:
			case T__42:
			case T__43:
			case T__44:
			case T__45:
			case T__46:
			case T__47:
			case T__48:
			case T__49:
			case T__50:
			case T__51:
			case T__52:
			case T__53:
			case T__54:
			case T__55:
			case T__56:
			case T__57:
			case T__58:
			case T__59:
			case T__60:
				enterOuterAlt(_localctx, 2);
				{
				setState(73);
				constraint();
				setState(74);
				match(EOF);
				}
				break;
			case STRING:
			case INT:
			case FLOAT:
			case BOOLEAN:
			case DATE:
			case TIME:
			case DATE_TIME:
			case ZONED_DATE_TIME:
			case NUMBER_RANGE:
			case DATE_TIME_RANGE:
			case ENUM:
			case LOCALE:
			case MULTIPLE_OPENING:
				enterOuterAlt(_localctx, 3);
				{
				setState(76);
				literal();
				setState(77);
				match(EOF);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class QueryContext extends ParserRuleContext {
		public ConstraintListArgsContext args;
		public ConstraintListArgsContext constraintListArgs() {
			return getRuleContext(ConstraintListArgsContext.class,0);
		}
		public QueryContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_query; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterQuery(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitQuery(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitQuery(this);
			else return visitor.visitChildren(this);
		}
	}

	public final QueryContext query() throws RecognitionException {
		QueryContext _localctx = new QueryContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_query);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(81);
			match(T__0);
			setState(82);
			((QueryContext)_localctx).args = constraintListArgs();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ConstraintContext extends ParserRuleContext {
		public HeadConstraintContext headConstraint() {
			return getRuleContext(HeadConstraintContext.class,0);
		}
		public FilterConstraintContext filterConstraint() {
			return getRuleContext(FilterConstraintContext.class,0);
		}
		public OrderConstraintContext orderConstraint() {
			return getRuleContext(OrderConstraintContext.class,0);
		}
		public RequireConstraintContext requireConstraint() {
			return getRuleContext(RequireConstraintContext.class,0);
		}
		public ConstraintContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constraint; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitConstraint(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstraintContext constraint() throws RecognitionException {
		ConstraintContext _localctx = new ConstraintContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_constraint);
		try {
			setState(88);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__1:
				enterOuterAlt(_localctx, 1);
				{
				setState(84);
				headConstraint();
				}
				break;
			case T__2:
			case T__3:
			case T__4:
			case T__5:
			case T__6:
			case T__7:
			case T__8:
			case T__9:
			case T__10:
			case T__11:
			case T__12:
			case T__13:
			case T__14:
			case T__15:
			case T__16:
			case T__17:
			case T__18:
			case T__19:
			case T__20:
			case T__21:
			case T__22:
			case T__23:
			case T__24:
			case T__25:
			case T__26:
			case T__27:
			case T__28:
			case T__29:
			case T__30:
			case T__31:
			case T__32:
			case T__33:
			case T__34:
				enterOuterAlt(_localctx, 2);
				{
				setState(85);
				filterConstraint();
				}
				break;
			case T__35:
			case T__36:
			case T__37:
			case T__38:
			case T__39:
			case T__40:
			case T__41:
				enterOuterAlt(_localctx, 3);
				{
				setState(86);
				orderConstraint();
				}
				break;
			case T__42:
			case T__43:
			case T__44:
			case T__45:
			case T__46:
			case T__47:
			case T__48:
			case T__49:
			case T__50:
			case T__51:
			case T__52:
			case T__53:
			case T__54:
			case T__55:
			case T__56:
			case T__57:
			case T__58:
			case T__59:
			case T__60:
				enterOuterAlt(_localctx, 4);
				{
				setState(87);
				requireConstraint();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class HeadConstraintContext extends ParserRuleContext {
		public HeadConstraintContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_headConstraint; }
	 
		public HeadConstraintContext() { }
		public void copyFrom(HeadConstraintContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class EntitiesConstraintContext extends HeadConstraintContext {
		public EntityTypeArgsContext args;
		public EntityTypeArgsContext entityTypeArgs() {
			return getRuleContext(EntityTypeArgsContext.class,0);
		}
		public EntitiesConstraintContext(HeadConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterEntitiesConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitEntitiesConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitEntitiesConstraint(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HeadConstraintContext headConstraint() throws RecognitionException {
		HeadConstraintContext _localctx = new HeadConstraintContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_headConstraint);
		try {
			_localctx = new EntitiesConstraintContext(_localctx);
			enterOuterAlt(_localctx, 1);
			{
			setState(90);
			match(T__1);
			setState(91);
			((EntitiesConstraintContext)_localctx).args = entityTypeArgs();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FilterConstraintContext extends ParserRuleContext {
		public FilterConstraintContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_filterConstraint; }
	 
		public FilterConstraintContext() { }
		public void copyFrom(FilterConstraintContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class ReferenceHavingAttributeConstraintContext extends FilterConstraintContext {
		public EntityTypeWithFilterConstraintArgsContext args;
		public EntityTypeWithFilterConstraintArgsContext entityTypeWithFilterConstraintArgs() {
			return getRuleContext(EntityTypeWithFilterConstraintArgsContext.class,0);
		}
		public ReferenceHavingAttributeConstraintContext(FilterConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterReferenceHavingAttributeConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitReferenceHavingAttributeConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitReferenceHavingAttributeConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class UserFilterConstraintContext extends FilterConstraintContext {
		public FilterConstraintContainerArgsContext args;
		public FilterConstraintContainerArgsContext filterConstraintContainerArgs() {
			return getRuleContext(FilterConstraintContainerArgsContext.class,0);
		}
		public UserFilterConstraintContext(FilterConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterUserFilterConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitUserFilterConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitUserFilterConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class InSetConstraintContext extends FilterConstraintContext {
		public NameWithValueListArgsContext args;
		public NameWithValueListArgsContext nameWithValueListArgs() {
			return getRuleContext(NameWithValueListArgsContext.class,0);
		}
		public InSetConstraintContext(FilterConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterInSetConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitInSetConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitInSetConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class StartsWithConstraintContext extends FilterConstraintContext {
		public NameWithValueArgsContext args;
		public NameWithValueArgsContext nameWithValueArgs() {
			return getRuleContext(NameWithValueArgsContext.class,0);
		}
		public StartsWithConstraintContext(FilterConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterStartsWithConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitStartsWithConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitStartsWithConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class IsNullConstraintContext extends FilterConstraintContext {
		public NameArgsContext args;
		public NameArgsContext nameArgs() {
			return getRuleContext(NameArgsContext.class,0);
		}
		public IsNullConstraintContext(FilterConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterIsNullConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitIsNullConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitIsNullConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class EndsWithConstraintContext extends FilterConstraintContext {
		public NameWithValueArgsContext args;
		public NameWithValueArgsContext nameWithValueArgs() {
			return getRuleContext(NameWithValueArgsContext.class,0);
		}
		public EndsWithConstraintContext(FilterConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterEndsWithConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitEndsWithConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitEndsWithConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PriceValidInConstraintContext extends FilterConstraintContext {
		public ValueArgsContext args;
		public ValueArgsContext valueArgs() {
			return getRuleContext(ValueArgsContext.class,0);
		}
		public PriceValidInConstraintContext(FilterConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterPriceValidInConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitPriceValidInConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitPriceValidInConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DirectRelationConstraintContext extends FilterConstraintContext {
		public EmptyArgsContext emptyArgs() {
			return getRuleContext(EmptyArgsContext.class,0);
		}
		public DirectRelationConstraintContext(FilterConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterDirectRelationConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitDirectRelationConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitDirectRelationConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class IsFalseConstraintContext extends FilterConstraintContext {
		public NameArgsContext args;
		public NameArgsContext nameArgs() {
			return getRuleContext(NameArgsContext.class,0);
		}
		public IsFalseConstraintContext(FilterConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterIsFalseConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitIsFalseConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitIsFalseConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class FilterByConstraintContext extends FilterConstraintContext {
		public FilterConstraintContainerArgsContext args;
		public FilterConstraintContainerArgsContext filterConstraintContainerArgs() {
			return getRuleContext(FilterConstraintContainerArgsContext.class,0);
		}
		public FilterByConstraintContext(FilterConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterFilterByConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitFilterByConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitFilterByConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PrimaryKeyConstraintContext extends FilterConstraintContext {
		public ValueListArgsContext args;
		public ValueListArgsContext valueListArgs() {
			return getRuleContext(ValueListArgsContext.class,0);
		}
		public PrimaryKeyConstraintContext(FilterConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterPrimaryKeyConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitPrimaryKeyConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitPrimaryKeyConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class BetweenConstraintContext extends FilterConstraintContext {
		public NameWithBetweenValuesArgsContext args;
		public NameWithBetweenValuesArgsContext nameWithBetweenValuesArgs() {
			return getRuleContext(NameWithBetweenValuesArgsContext.class,0);
		}
		public BetweenConstraintContext(FilterConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterBetweenConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitBetweenConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitBetweenConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class LanguageConstraintContext extends FilterConstraintContext {
		public ValueArgsContext args;
		public ValueArgsContext valueArgs() {
			return getRuleContext(ValueArgsContext.class,0);
		}
		public LanguageConstraintContext(FilterConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterLanguageConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitLanguageConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitLanguageConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PriceInPriceListsConstraintsContext extends FilterConstraintContext {
		public ValueListArgsContext args;
		public ValueListArgsContext valueListArgs() {
			return getRuleContext(ValueListArgsContext.class,0);
		}
		public PriceInPriceListsConstraintsContext(FilterConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterPriceInPriceListsConstraints(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitPriceInPriceListsConstraints(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitPriceInPriceListsConstraints(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class GreaterThanEqualsConstraintContext extends FilterConstraintContext {
		public NameWithValueArgsContext args;
		public NameWithValueArgsContext nameWithValueArgs() {
			return getRuleContext(NameWithValueArgsContext.class,0);
		}
		public GreaterThanEqualsConstraintContext(FilterConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterGreaterThanEqualsConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitGreaterThanEqualsConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitGreaterThanEqualsConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ExcludingConstraintContext extends FilterConstraintContext {
		public ValueListArgsContext args;
		public ValueListArgsContext valueListArgs() {
			return getRuleContext(ValueListArgsContext.class,0);
		}
		public ExcludingConstraintContext(FilterConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterExcludingConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitExcludingConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitExcludingConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class LessThanEqualsConstraintContext extends FilterConstraintContext {
		public NameWithValueArgsContext args;
		public NameWithValueArgsContext nameWithValueArgs() {
			return getRuleContext(NameWithValueArgsContext.class,0);
		}
		public LessThanEqualsConstraintContext(FilterConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterLessThanEqualsConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitLessThanEqualsConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitLessThanEqualsConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class GreaterThanConstraintContext extends FilterConstraintContext {
		public NameWithValueArgsContext args;
		public NameWithValueArgsContext nameWithValueArgs() {
			return getRuleContext(NameWithValueArgsContext.class,0);
		}
		public GreaterThanConstraintContext(FilterConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterGreaterThanConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitGreaterThanConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitGreaterThanConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AndConstraintContext extends FilterConstraintContext {
		public FilterConstraintContainerArgsContext args;
		public FilterConstraintContainerArgsContext filterConstraintContainerArgs() {
			return getRuleContext(FilterConstraintContainerArgsContext.class,0);
		}
		public AndConstraintContext(FilterConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterAndConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitAndConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitAndConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class IsNotNullConstraintContext extends FilterConstraintContext {
		public NameArgsContext args;
		public NameArgsContext nameArgs() {
			return getRuleContext(NameArgsContext.class,0);
		}
		public IsNotNullConstraintContext(FilterConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterIsNotNullConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitIsNotNullConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitIsNotNullConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PriceInCurrencyConstraintContext extends FilterConstraintContext {
		public ValueArgsContext args;
		public ValueArgsContext valueArgs() {
			return getRuleContext(ValueArgsContext.class,0);
		}
		public PriceInCurrencyConstraintContext(FilterConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterPriceInCurrencyConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitPriceInCurrencyConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitPriceInCurrencyConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ContainsConstraintContext extends FilterConstraintContext {
		public NameWithValueArgsContext args;
		public NameWithValueArgsContext nameWithValueArgs() {
			return getRuleContext(NameWithValueArgsContext.class,0);
		}
		public ContainsConstraintContext(FilterConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterContainsConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitContainsConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitContainsConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class LessThanConstraintContext extends FilterConstraintContext {
		public NameWithValueArgsContext args;
		public NameWithValueArgsContext nameWithValueArgs() {
			return getRuleContext(NameWithValueArgsContext.class,0);
		}
		public LessThanConstraintContext(FilterConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterLessThanConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitLessThanConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitLessThanConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class FacetConstraintContext extends FilterConstraintContext {
		public EntityTypeWithValueListArgsContext args;
		public EntityTypeWithValueListArgsContext entityTypeWithValueListArgs() {
			return getRuleContext(EntityTypeWithValueListArgsContext.class,0);
		}
		public FacetConstraintContext(FilterConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterFacetConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitFacetConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitFacetConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PriceBetweenConstraintContext extends FilterConstraintContext {
		public BetweenValuesArgsContext args;
		public BetweenValuesArgsContext betweenValuesArgs() {
			return getRuleContext(BetweenValuesArgsContext.class,0);
		}
		public PriceBetweenConstraintContext(FilterConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterPriceBetweenConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitPriceBetweenConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitPriceBetweenConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class WithinRootHierarchyConstraintContext extends FilterConstraintContext {
		public WithinRootHierarchyConstraintArgsContext args;
		public WithinRootHierarchyConstraintArgsContext withinRootHierarchyConstraintArgs() {
			return getRuleContext(WithinRootHierarchyConstraintArgsContext.class,0);
		}
		public WithinRootHierarchyConstraintContext(FilterConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterWithinRootHierarchyConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitWithinRootHierarchyConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitWithinRootHierarchyConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class EqualsConstraintContext extends FilterConstraintContext {
		public NameWithValueArgsContext args;
		public NameWithValueArgsContext nameWithValueArgs() {
			return getRuleContext(NameWithValueArgsContext.class,0);
		}
		public EqualsConstraintContext(FilterConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterEqualsConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitEqualsConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitEqualsConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class InRangeConstraintContext extends FilterConstraintContext {
		public NameWithValueArgsContext args;
		public NameWithValueArgsContext nameWithValueArgs() {
			return getRuleContext(NameWithValueArgsContext.class,0);
		}
		public InRangeConstraintContext(FilterConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterInRangeConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitInRangeConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitInRangeConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class IsTrueConstraintContext extends FilterConstraintContext {
		public NameArgsContext args;
		public NameArgsContext nameArgs() {
			return getRuleContext(NameArgsContext.class,0);
		}
		public IsTrueConstraintContext(FilterConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterIsTrueConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitIsTrueConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitIsTrueConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class WithinHierarchyConstraintContext extends FilterConstraintContext {
		public WithinHierarchyConstraintArgsContext args;
		public WithinHierarchyConstraintArgsContext withinHierarchyConstraintArgs() {
			return getRuleContext(WithinHierarchyConstraintArgsContext.class,0);
		}
		public WithinHierarchyConstraintContext(FilterConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterWithinHierarchyConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitWithinHierarchyConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitWithinHierarchyConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ExcludingRootConstraintContext extends FilterConstraintContext {
		public EmptyArgsContext emptyArgs() {
			return getRuleContext(EmptyArgsContext.class,0);
		}
		public ExcludingRootConstraintContext(FilterConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterExcludingRootConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitExcludingRootConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitExcludingRootConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class OrConstraintContext extends FilterConstraintContext {
		public FilterConstraintContainerArgsContext args;
		public FilterConstraintContainerArgsContext filterConstraintContainerArgs() {
			return getRuleContext(FilterConstraintContainerArgsContext.class,0);
		}
		public OrConstraintContext(FilterConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterOrConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitOrConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitOrConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class NotConstraintContext extends FilterConstraintContext {
		public FilterConstraintContainerArgsContext args;
		public FilterConstraintContainerArgsContext filterConstraintContainerArgs() {
			return getRuleContext(FilterConstraintContainerArgsContext.class,0);
		}
		public NotConstraintContext(FilterConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterNotConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitNotConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitNotConstraint(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FilterConstraintContext filterConstraint() throws RecognitionException {
		FilterConstraintContext _localctx = new FilterConstraintContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_filterConstraint);
		try {
			setState(159);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__2:
				_localctx = new FilterByConstraintContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(93);
				match(T__2);
				setState(94);
				((FilterByConstraintContext)_localctx).args = filterConstraintContainerArgs();
				}
				break;
			case T__3:
				_localctx = new AndConstraintContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(95);
				match(T__3);
				setState(96);
				((AndConstraintContext)_localctx).args = filterConstraintContainerArgs();
				}
				break;
			case T__4:
				_localctx = new OrConstraintContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(97);
				match(T__4);
				setState(98);
				((OrConstraintContext)_localctx).args = filterConstraintContainerArgs();
				}
				break;
			case T__5:
				_localctx = new NotConstraintContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(99);
				match(T__5);
				setState(100);
				((NotConstraintContext)_localctx).args = filterConstraintContainerArgs();
				}
				break;
			case T__6:
				_localctx = new UserFilterConstraintContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(101);
				match(T__6);
				setState(102);
				((UserFilterConstraintContext)_localctx).args = filterConstraintContainerArgs();
				}
				break;
			case T__7:
				_localctx = new EqualsConstraintContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(103);
				match(T__7);
				setState(104);
				((EqualsConstraintContext)_localctx).args = nameWithValueArgs();
				}
				break;
			case T__8:
				_localctx = new GreaterThanConstraintContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(105);
				match(T__8);
				setState(106);
				((GreaterThanConstraintContext)_localctx).args = nameWithValueArgs();
				}
				break;
			case T__9:
				_localctx = new GreaterThanEqualsConstraintContext(_localctx);
				enterOuterAlt(_localctx, 8);
				{
				setState(107);
				match(T__9);
				setState(108);
				((GreaterThanEqualsConstraintContext)_localctx).args = nameWithValueArgs();
				}
				break;
			case T__10:
				_localctx = new LessThanConstraintContext(_localctx);
				enterOuterAlt(_localctx, 9);
				{
				setState(109);
				match(T__10);
				setState(110);
				((LessThanConstraintContext)_localctx).args = nameWithValueArgs();
				}
				break;
			case T__11:
				_localctx = new LessThanEqualsConstraintContext(_localctx);
				enterOuterAlt(_localctx, 10);
				{
				setState(111);
				match(T__11);
				setState(112);
				((LessThanEqualsConstraintContext)_localctx).args = nameWithValueArgs();
				}
				break;
			case T__12:
				_localctx = new BetweenConstraintContext(_localctx);
				enterOuterAlt(_localctx, 11);
				{
				setState(113);
				match(T__12);
				setState(114);
				((BetweenConstraintContext)_localctx).args = nameWithBetweenValuesArgs();
				}
				break;
			case T__13:
				_localctx = new InSetConstraintContext(_localctx);
				enterOuterAlt(_localctx, 12);
				{
				setState(115);
				match(T__13);
				setState(116);
				((InSetConstraintContext)_localctx).args = nameWithValueListArgs();
				}
				break;
			case T__14:
				_localctx = new ContainsConstraintContext(_localctx);
				enterOuterAlt(_localctx, 13);
				{
				setState(117);
				match(T__14);
				setState(118);
				((ContainsConstraintContext)_localctx).args = nameWithValueArgs();
				}
				break;
			case T__15:
				_localctx = new StartsWithConstraintContext(_localctx);
				enterOuterAlt(_localctx, 14);
				{
				setState(119);
				match(T__15);
				setState(120);
				((StartsWithConstraintContext)_localctx).args = nameWithValueArgs();
				}
				break;
			case T__16:
				_localctx = new EndsWithConstraintContext(_localctx);
				enterOuterAlt(_localctx, 15);
				{
				setState(121);
				match(T__16);
				setState(122);
				((EndsWithConstraintContext)_localctx).args = nameWithValueArgs();
				}
				break;
			case T__17:
				_localctx = new IsTrueConstraintContext(_localctx);
				enterOuterAlt(_localctx, 16);
				{
				setState(123);
				match(T__17);
				setState(124);
				((IsTrueConstraintContext)_localctx).args = nameArgs();
				}
				break;
			case T__18:
				_localctx = new IsFalseConstraintContext(_localctx);
				enterOuterAlt(_localctx, 17);
				{
				setState(125);
				match(T__18);
				setState(126);
				((IsFalseConstraintContext)_localctx).args = nameArgs();
				}
				break;
			case T__19:
				_localctx = new IsNullConstraintContext(_localctx);
				enterOuterAlt(_localctx, 18);
				{
				setState(127);
				match(T__19);
				setState(128);
				((IsNullConstraintContext)_localctx).args = nameArgs();
				}
				break;
			case T__20:
				_localctx = new IsNotNullConstraintContext(_localctx);
				enterOuterAlt(_localctx, 19);
				{
				setState(129);
				match(T__20);
				setState(130);
				((IsNotNullConstraintContext)_localctx).args = nameArgs();
				}
				break;
			case T__21:
				_localctx = new InRangeConstraintContext(_localctx);
				enterOuterAlt(_localctx, 20);
				{
				setState(131);
				match(T__21);
				setState(132);
				((InRangeConstraintContext)_localctx).args = nameWithValueArgs();
				}
				break;
			case T__22:
				_localctx = new PrimaryKeyConstraintContext(_localctx);
				enterOuterAlt(_localctx, 21);
				{
				setState(133);
				match(T__22);
				setState(134);
				((PrimaryKeyConstraintContext)_localctx).args = valueListArgs();
				}
				break;
			case T__23:
				_localctx = new LanguageConstraintContext(_localctx);
				enterOuterAlt(_localctx, 22);
				{
				setState(135);
				match(T__23);
				setState(136);
				((LanguageConstraintContext)_localctx).args = valueArgs();
				}
				break;
			case T__24:
				_localctx = new PriceInCurrencyConstraintContext(_localctx);
				enterOuterAlt(_localctx, 23);
				{
				setState(137);
				match(T__24);
				setState(138);
				((PriceInCurrencyConstraintContext)_localctx).args = valueArgs();
				}
				break;
			case T__25:
				_localctx = new PriceInPriceListsConstraintsContext(_localctx);
				enterOuterAlt(_localctx, 24);
				{
				setState(139);
				match(T__25);
				setState(140);
				((PriceInPriceListsConstraintsContext)_localctx).args = valueListArgs();
				}
				break;
			case T__26:
				_localctx = new PriceValidInConstraintContext(_localctx);
				enterOuterAlt(_localctx, 25);
				{
				setState(141);
				match(T__26);
				setState(142);
				((PriceValidInConstraintContext)_localctx).args = valueArgs();
				}
				break;
			case T__27:
				_localctx = new PriceBetweenConstraintContext(_localctx);
				enterOuterAlt(_localctx, 26);
				{
				setState(143);
				match(T__27);
				setState(144);
				((PriceBetweenConstraintContext)_localctx).args = betweenValuesArgs();
				}
				break;
			case T__28:
				_localctx = new FacetConstraintContext(_localctx);
				enterOuterAlt(_localctx, 27);
				{
				setState(145);
				match(T__28);
				setState(146);
				((FacetConstraintContext)_localctx).args = entityTypeWithValueListArgs();
				}
				break;
			case T__29:
				_localctx = new ReferenceHavingAttributeConstraintContext(_localctx);
				enterOuterAlt(_localctx, 28);
				{
				setState(147);
				match(T__29);
				setState(148);
				((ReferenceHavingAttributeConstraintContext)_localctx).args = entityTypeWithFilterConstraintArgs();
				}
				break;
			case T__30:
				_localctx = new WithinHierarchyConstraintContext(_localctx);
				enterOuterAlt(_localctx, 29);
				{
				setState(149);
				match(T__30);
				setState(150);
				((WithinHierarchyConstraintContext)_localctx).args = withinHierarchyConstraintArgs();
				}
				break;
			case T__31:
				_localctx = new WithinRootHierarchyConstraintContext(_localctx);
				enterOuterAlt(_localctx, 30);
				{
				setState(151);
				match(T__31);
				setState(152);
				((WithinRootHierarchyConstraintContext)_localctx).args = withinRootHierarchyConstraintArgs();
				}
				break;
			case T__32:
				_localctx = new DirectRelationConstraintContext(_localctx);
				enterOuterAlt(_localctx, 31);
				{
				setState(153);
				match(T__32);
				setState(154);
				emptyArgs();
				}
				break;
			case T__33:
				_localctx = new ExcludingRootConstraintContext(_localctx);
				enterOuterAlt(_localctx, 32);
				{
				setState(155);
				match(T__33);
				setState(156);
				emptyArgs();
				}
				break;
			case T__34:
				_localctx = new ExcludingConstraintContext(_localctx);
				enterOuterAlt(_localctx, 33);
				{
				setState(157);
				match(T__34);
				setState(158);
				((ExcludingConstraintContext)_localctx).args = valueListArgs();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OrderConstraintContext extends ParserRuleContext {
		public OrderConstraintContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_orderConstraint; }
	 
		public OrderConstraintContext() { }
		public void copyFrom(OrderConstraintContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class RandomConstraintContext extends OrderConstraintContext {
		public EmptyArgsContext emptyArgs() {
			return getRuleContext(EmptyArgsContext.class,0);
		}
		public RandomConstraintContext(OrderConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterRandomConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitRandomConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitRandomConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DescendingConstraintContext extends OrderConstraintContext {
		public NameArgsContext args;
		public NameArgsContext nameArgs() {
			return getRuleContext(NameArgsContext.class,0);
		}
		public DescendingConstraintContext(OrderConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterDescendingConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitDescendingConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitDescendingConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AscendingConstraintContext extends OrderConstraintContext {
		public NameArgsContext args;
		public NameArgsContext nameArgs() {
			return getRuleContext(NameArgsContext.class,0);
		}
		public AscendingConstraintContext(OrderConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterAscendingConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitAscendingConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitAscendingConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class OrderByConstraintContext extends OrderConstraintContext {
		public OrderConstraintContainerArgsContext args;
		public OrderConstraintContainerArgsContext orderConstraintContainerArgs() {
			return getRuleContext(OrderConstraintContainerArgsContext.class,0);
		}
		public OrderByConstraintContext(OrderConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterOrderByConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitOrderByConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitOrderByConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ReferenceAttributeConstraintContext extends OrderConstraintContext {
		public EntityTypeWithOrderConstraintListArgsContext args;
		public EntityTypeWithOrderConstraintListArgsContext entityTypeWithOrderConstraintListArgs() {
			return getRuleContext(EntityTypeWithOrderConstraintListArgsContext.class,0);
		}
		public ReferenceAttributeConstraintContext(OrderConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterReferenceAttributeConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitReferenceAttributeConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitReferenceAttributeConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PriceAscendingConstraintContext extends OrderConstraintContext {
		public EmptyArgsContext emptyArgs() {
			return getRuleContext(EmptyArgsContext.class,0);
		}
		public PriceAscendingConstraintContext(OrderConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterPriceAscendingConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitPriceAscendingConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitPriceAscendingConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PriceDescendingConstraintContext extends OrderConstraintContext {
		public EmptyArgsContext emptyArgs() {
			return getRuleContext(EmptyArgsContext.class,0);
		}
		public PriceDescendingConstraintContext(OrderConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterPriceDescendingConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitPriceDescendingConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitPriceDescendingConstraint(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OrderConstraintContext orderConstraint() throws RecognitionException {
		OrderConstraintContext _localctx = new OrderConstraintContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_orderConstraint);
		try {
			setState(175);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__35:
				_localctx = new OrderByConstraintContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(161);
				match(T__35);
				setState(162);
				((OrderByConstraintContext)_localctx).args = orderConstraintContainerArgs();
				}
				break;
			case T__36:
				_localctx = new AscendingConstraintContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(163);
				match(T__36);
				setState(164);
				((AscendingConstraintContext)_localctx).args = nameArgs();
				}
				break;
			case T__37:
				_localctx = new DescendingConstraintContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(165);
				match(T__37);
				setState(166);
				((DescendingConstraintContext)_localctx).args = nameArgs();
				}
				break;
			case T__38:
				_localctx = new PriceAscendingConstraintContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(167);
				match(T__38);
				setState(168);
				emptyArgs();
				}
				break;
			case T__39:
				_localctx = new PriceDescendingConstraintContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(169);
				match(T__39);
				setState(170);
				emptyArgs();
				}
				break;
			case T__40:
				_localctx = new RandomConstraintContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(171);
				match(T__40);
				setState(172);
				emptyArgs();
				}
				break;
			case T__41:
				_localctx = new ReferenceAttributeConstraintContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(173);
				match(T__41);
				setState(174);
				((ReferenceAttributeConstraintContext)_localctx).args = entityTypeWithOrderConstraintListArgs();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RequireConstraintContext extends ParserRuleContext {
		public RequireConstraintContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_requireConstraint; }
	 
		public RequireConstraintContext() { }
		public void copyFrom(RequireConstraintContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class AssociatedDataConstraintContext extends RequireConstraintContext {
		public NameListArgsContext args;
		public EmptyArgsContext emptyArgs() {
			return getRuleContext(EmptyArgsContext.class,0);
		}
		public NameListArgsContext nameListArgs() {
			return getRuleContext(NameListArgsContext.class,0);
		}
		public AssociatedDataConstraintContext(RequireConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterAssociatedDataConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitAssociatedDataConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitAssociatedDataConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class HierarchyStatisticsConstraintContext extends RequireConstraintContext {
		public EntityTypeWithRequireConstraintListArgsContext args;
		public EntityTypeWithRequireConstraintListArgsContext entityTypeWithRequireConstraintListArgs() {
			return getRuleContext(EntityTypeWithRequireConstraintListArgsContext.class,0);
		}
		public HierarchyStatisticsConstraintContext(RequireConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterHierarchyStatisticsConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitHierarchyStatisticsConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitHierarchyStatisticsConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DataInLanguageConstraintContext extends RequireConstraintContext {
		public ValueListArgsContext args;
		public EmptyArgsContext emptyArgs() {
			return getRuleContext(EmptyArgsContext.class,0);
		}
		public ValueListArgsContext valueListArgs() {
			return getRuleContext(ValueListArgsContext.class,0);
		}
		public DataInLanguageConstraintContext(RequireConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterDataInLanguageConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitDataInLanguageConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitDataInLanguageConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class RequireContainerConstraintContext extends RequireConstraintContext {
		public RequireConstraintContainerArgsContext args;
		public RequireConstraintContainerArgsContext requireConstraintContainerArgs() {
			return getRuleContext(RequireConstraintContainerArgsContext.class,0);
		}
		public RequireContainerConstraintContext(RequireConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterRequireContainerConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitRequireContainerConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitRequireContainerConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ParentsConstraintContext extends RequireConstraintContext {
		public RequireConstraintContainerArgsContext args;
		public EmptyArgsContext emptyArgs() {
			return getRuleContext(EmptyArgsContext.class,0);
		}
		public RequireConstraintContainerArgsContext requireConstraintContainerArgs() {
			return getRuleContext(RequireConstraintContainerArgsContext.class,0);
		}
		public ParentsConstraintContext(RequireConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterParentsConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitParentsConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitParentsConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class FacetGroupsDisjunctionConstraintContext extends RequireConstraintContext {
		public EntityTypeWithValueListArgsContext args;
		public EntityTypeWithValueListArgsContext entityTypeWithValueListArgs() {
			return getRuleContext(EntityTypeWithValueListArgsContext.class,0);
		}
		public FacetGroupsDisjunctionConstraintContext(RequireConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterFacetGroupsDisjunctionConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitFacetGroupsDisjunctionConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitFacetGroupsDisjunctionConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PriceHistogramConstraintContext extends RequireConstraintContext {
		public ValueArgsContext args;
		public ValueArgsContext valueArgs() {
			return getRuleContext(ValueArgsContext.class,0);
		}
		public PriceHistogramConstraintContext(RequireConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterPriceHistogramConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitPriceHistogramConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitPriceHistogramConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PricesConstraintContext extends RequireConstraintContext {
		public ValueArgsContext args;
		public EmptyArgsContext emptyArgs() {
			return getRuleContext(EmptyArgsContext.class,0);
		}
		public ValueArgsContext valueArgs() {
			return getRuleContext(ValueArgsContext.class,0);
		}
		public PricesConstraintContext(RequireConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterPricesConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitPricesConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitPricesConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ParentsOfTypeConstraintContext extends RequireConstraintContext {
		public ParentsOfTypeConstraintArgsContext args;
		public ParentsOfTypeConstraintArgsContext parentsOfTypeConstraintArgs() {
			return getRuleContext(ParentsOfTypeConstraintArgsContext.class,0);
		}
		public ParentsOfTypeConstraintContext(RequireConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterParentsOfTypeConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitParentsOfTypeConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitParentsOfTypeConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AttributesConstraintContext extends RequireConstraintContext {
		public EmptyArgsContext emptyArgs() {
			return getRuleContext(EmptyArgsContext.class,0);
		}
		public AttributesConstraintContext(RequireConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterAttributesConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitAttributesConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitAttributesConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class EntityBodyConstraintContext extends RequireConstraintContext {
		public EmptyArgsContext emptyArgs() {
			return getRuleContext(EmptyArgsContext.class,0);
		}
		public EntityBodyConstraintContext(RequireConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterEntityBodyConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitEntityBodyConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitEntityBodyConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class FacetGroupsConjunctionConstraintContext extends RequireConstraintContext {
		public EntityTypeWithValueListArgsContext args;
		public EntityTypeWithValueListArgsContext entityTypeWithValueListArgs() {
			return getRuleContext(EntityTypeWithValueListArgsContext.class,0);
		}
		public FacetGroupsConjunctionConstraintContext(RequireConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterFacetGroupsConjunctionConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitFacetGroupsConjunctionConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitFacetGroupsConjunctionConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PageConstraintContext extends RequireConstraintContext {
		public PageConstraintArgsContext args;
		public PageConstraintArgsContext pageConstraintArgs() {
			return getRuleContext(PageConstraintArgsContext.class,0);
		}
		public PageConstraintContext(RequireConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterPageConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitPageConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitPageConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class StripConstraintContext extends RequireConstraintContext {
		public StripConstraintArgsContext args;
		public StripConstraintArgsContext stripConstraintArgs() {
			return getRuleContext(StripConstraintArgsContext.class,0);
		}
		public StripConstraintContext(RequireConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterStripConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitStripConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitStripConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AttributeHistogramConstraintContext extends RequireConstraintContext {
		public ValueWithNameListArgsContext args;
		public ValueWithNameListArgsContext valueWithNameListArgs() {
			return getRuleContext(ValueWithNameListArgsContext.class,0);
		}
		public AttributeHistogramConstraintContext(RequireConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterAttributeHistogramConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitAttributeHistogramConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitAttributeHistogramConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class UseOfPriceConstraintContext extends RequireConstraintContext {
		public ValueArgsContext args;
		public ValueArgsContext valueArgs() {
			return getRuleContext(ValueArgsContext.class,0);
		}
		public UseOfPriceConstraintContext(RequireConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterUseOfPriceConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitUseOfPriceConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitUseOfPriceConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ReferencesConstraintContext extends RequireConstraintContext {
		public ReferencedTypesArgsContext args;
		public EmptyArgsContext emptyArgs() {
			return getRuleContext(EmptyArgsContext.class,0);
		}
		public ReferencedTypesArgsContext referencedTypesArgs() {
			return getRuleContext(ReferencedTypesArgsContext.class,0);
		}
		public ReferencesConstraintContext(RequireConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterReferencesConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitReferencesConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitReferencesConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class FacetSummaryConstraintContext extends RequireConstraintContext {
		public ValueArgsContext args;
		public EmptyArgsContext emptyArgs() {
			return getRuleContext(EmptyArgsContext.class,0);
		}
		public ValueArgsContext valueArgs() {
			return getRuleContext(ValueArgsContext.class,0);
		}
		public FacetSummaryConstraintContext(RequireConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterFacetSummaryConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitFacetSummaryConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitFacetSummaryConstraint(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class FacetGroupsNegationConstraintContext extends RequireConstraintContext {
		public EntityTypeWithValueListArgsContext args;
		public EntityTypeWithValueListArgsContext entityTypeWithValueListArgs() {
			return getRuleContext(EntityTypeWithValueListArgsContext.class,0);
		}
		public FacetGroupsNegationConstraintContext(RequireConstraintContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterFacetGroupsNegationConstraint(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitFacetGroupsNegationConstraint(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitFacetGroupsNegationConstraint(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RequireConstraintContext requireConstraint() throws RecognitionException {
		RequireConstraintContext _localctx = new RequireConstraintContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_requireConstraint);
		try {
			setState(233);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__42:
				_localctx = new RequireContainerConstraintContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(177);
				match(T__42);
				setState(178);
				((RequireContainerConstraintContext)_localctx).args = requireConstraintContainerArgs();
				}
				break;
			case T__43:
				_localctx = new PageConstraintContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(179);
				match(T__43);
				setState(180);
				((PageConstraintContext)_localctx).args = pageConstraintArgs();
				}
				break;
			case T__44:
				_localctx = new StripConstraintContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(181);
				match(T__44);
				setState(182);
				((StripConstraintContext)_localctx).args = stripConstraintArgs();
				}
				break;
			case T__45:
				_localctx = new EntityBodyConstraintContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(183);
				match(T__45);
				setState(184);
				emptyArgs();
				}
				break;
			case T__46:
				_localctx = new AttributesConstraintContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(185);
				match(T__46);
				setState(186);
				emptyArgs();
				}
				break;
			case T__47:
				_localctx = new PricesConstraintContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(187);
				match(T__47);
				setState(190);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,4,_ctx) ) {
				case 1:
					{
					setState(188);
					emptyArgs();
					}
					break;
				case 2:
					{
					setState(189);
					((PricesConstraintContext)_localctx).args = valueArgs();
					}
					break;
				}
				}
				break;
			case T__48:
				_localctx = new AssociatedDataConstraintContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(192);
				match(T__48);
				setState(195);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,5,_ctx) ) {
				case 1:
					{
					setState(193);
					emptyArgs();
					}
					break;
				case 2:
					{
					setState(194);
					((AssociatedDataConstraintContext)_localctx).args = nameListArgs();
					}
					break;
				}
				}
				break;
			case T__49:
				_localctx = new ReferencesConstraintContext(_localctx);
				enterOuterAlt(_localctx, 8);
				{
				setState(197);
				match(T__49);
				setState(200);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,6,_ctx) ) {
				case 1:
					{
					setState(198);
					emptyArgs();
					}
					break;
				case 2:
					{
					setState(199);
					((ReferencesConstraintContext)_localctx).args = referencedTypesArgs();
					}
					break;
				}
				}
				break;
			case T__50:
				_localctx = new UseOfPriceConstraintContext(_localctx);
				enterOuterAlt(_localctx, 9);
				{
				setState(202);
				match(T__50);
				setState(203);
				((UseOfPriceConstraintContext)_localctx).args = valueArgs();
				}
				break;
			case T__51:
				_localctx = new DataInLanguageConstraintContext(_localctx);
				enterOuterAlt(_localctx, 10);
				{
				setState(204);
				match(T__51);
				setState(207);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
				case 1:
					{
					setState(205);
					emptyArgs();
					}
					break;
				case 2:
					{
					setState(206);
					((DataInLanguageConstraintContext)_localctx).args = valueListArgs();
					}
					break;
				}
				}
				break;
			case T__52:
				_localctx = new ParentsConstraintContext(_localctx);
				enterOuterAlt(_localctx, 11);
				{
				setState(209);
				match(T__52);
				setState(212);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,8,_ctx) ) {
				case 1:
					{
					setState(210);
					emptyArgs();
					}
					break;
				case 2:
					{
					setState(211);
					((ParentsConstraintContext)_localctx).args = requireConstraintContainerArgs();
					}
					break;
				}
				}
				break;
			case T__53:
				_localctx = new ParentsOfTypeConstraintContext(_localctx);
				enterOuterAlt(_localctx, 12);
				{
				setState(214);
				match(T__53);
				setState(215);
				((ParentsOfTypeConstraintContext)_localctx).args = parentsOfTypeConstraintArgs();
				}
				break;
			case T__54:
				_localctx = new FacetSummaryConstraintContext(_localctx);
				enterOuterAlt(_localctx, 13);
				{
				setState(216);
				match(T__54);
				setState(219);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,9,_ctx) ) {
				case 1:
					{
					setState(217);
					emptyArgs();
					}
					break;
				case 2:
					{
					setState(218);
					((FacetSummaryConstraintContext)_localctx).args = valueArgs();
					}
					break;
				}
				}
				break;
			case T__55:
				_localctx = new FacetGroupsConjunctionConstraintContext(_localctx);
				enterOuterAlt(_localctx, 14);
				{
				setState(221);
				match(T__55);
				setState(222);
				((FacetGroupsConjunctionConstraintContext)_localctx).args = entityTypeWithValueListArgs();
				}
				break;
			case T__56:
				_localctx = new FacetGroupsDisjunctionConstraintContext(_localctx);
				enterOuterAlt(_localctx, 15);
				{
				setState(223);
				match(T__56);
				setState(224);
				((FacetGroupsDisjunctionConstraintContext)_localctx).args = entityTypeWithValueListArgs();
				}
				break;
			case T__57:
				_localctx = new FacetGroupsNegationConstraintContext(_localctx);
				enterOuterAlt(_localctx, 16);
				{
				setState(225);
				match(T__57);
				setState(226);
				((FacetGroupsNegationConstraintContext)_localctx).args = entityTypeWithValueListArgs();
				}
				break;
			case T__58:
				_localctx = new AttributeHistogramConstraintContext(_localctx);
				enterOuterAlt(_localctx, 17);
				{
				setState(227);
				match(T__58);
				setState(228);
				((AttributeHistogramConstraintContext)_localctx).args = valueWithNameListArgs();
				}
				break;
			case T__59:
				_localctx = new PriceHistogramConstraintContext(_localctx);
				enterOuterAlt(_localctx, 18);
				{
				setState(229);
				match(T__59);
				setState(230);
				((PriceHistogramConstraintContext)_localctx).args = valueArgs();
				}
				break;
			case T__60:
				_localctx = new HierarchyStatisticsConstraintContext(_localctx);
				enterOuterAlt(_localctx, 19);
				{
				setState(231);
				match(T__60);
				setState(232);
				((HierarchyStatisticsConstraintContext)_localctx).args = entityTypeWithRequireConstraintListArgs();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ConstraintListArgsContext extends ParserRuleContext {
		public ConstraintContext constraint;
		public List<ConstraintContext> constraints = new ArrayList<ConstraintContext>();
		public TerminalNode ARGS_OPENING() { return getToken(EvitaQLParser.ARGS_OPENING, 0); }
		public TerminalNode ARGS_CLOSING() { return getToken(EvitaQLParser.ARGS_CLOSING, 0); }
		public List<ConstraintContext> constraint() {
			return getRuleContexts(ConstraintContext.class);
		}
		public ConstraintContext constraint(int i) {
			return getRuleContext(ConstraintContext.class,i);
		}
		public List<TerminalNode> ARGS_DELIMITER() { return getTokens(EvitaQLParser.ARGS_DELIMITER); }
		public TerminalNode ARGS_DELIMITER(int i) {
			return getToken(EvitaQLParser.ARGS_DELIMITER, i);
		}
		public ConstraintListArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_constraintListArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterConstraintListArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitConstraintListArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitConstraintListArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ConstraintListArgsContext constraintListArgs() throws RecognitionException {
		ConstraintListArgsContext _localctx = new ConstraintListArgsContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_constraintListArgs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(235);
			match(ARGS_OPENING);
			setState(236);
			((ConstraintListArgsContext)_localctx).constraint = constraint();
			((ConstraintListArgsContext)_localctx).constraints.add(((ConstraintListArgsContext)_localctx).constraint);
			setState(241);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ARGS_DELIMITER) {
				{
				{
				setState(237);
				match(ARGS_DELIMITER);
				setState(238);
				((ConstraintListArgsContext)_localctx).constraint = constraint();
				((ConstraintListArgsContext)_localctx).constraints.add(((ConstraintListArgsContext)_localctx).constraint);
				}
				}
				setState(243);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(244);
			match(ARGS_CLOSING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class EmptyArgsContext extends ParserRuleContext {
		public TerminalNode ARGS_OPENING() { return getToken(EvitaQLParser.ARGS_OPENING, 0); }
		public TerminalNode ARGS_CLOSING() { return getToken(EvitaQLParser.ARGS_CLOSING, 0); }
		public EmptyArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_emptyArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterEmptyArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitEmptyArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitEmptyArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EmptyArgsContext emptyArgs() throws RecognitionException {
		EmptyArgsContext _localctx = new EmptyArgsContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_emptyArgs);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(246);
			match(ARGS_OPENING);
			setState(247);
			match(ARGS_CLOSING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FilterConstraintContainerArgsContext extends ParserRuleContext {
		public FilterConstraintContext filterConstraint;
		public List<FilterConstraintContext> constraints = new ArrayList<FilterConstraintContext>();
		public TerminalNode ARGS_OPENING() { return getToken(EvitaQLParser.ARGS_OPENING, 0); }
		public TerminalNode ARGS_CLOSING() { return getToken(EvitaQLParser.ARGS_CLOSING, 0); }
		public List<FilterConstraintContext> filterConstraint() {
			return getRuleContexts(FilterConstraintContext.class);
		}
		public FilterConstraintContext filterConstraint(int i) {
			return getRuleContext(FilterConstraintContext.class,i);
		}
		public List<TerminalNode> ARGS_DELIMITER() { return getTokens(EvitaQLParser.ARGS_DELIMITER); }
		public TerminalNode ARGS_DELIMITER(int i) {
			return getToken(EvitaQLParser.ARGS_DELIMITER, i);
		}
		public FilterConstraintContainerArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_filterConstraintContainerArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterFilterConstraintContainerArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitFilterConstraintContainerArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitFilterConstraintContainerArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FilterConstraintContainerArgsContext filterConstraintContainerArgs() throws RecognitionException {
		FilterConstraintContainerArgsContext _localctx = new FilterConstraintContainerArgsContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_filterConstraintContainerArgs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(249);
			match(ARGS_OPENING);
			setState(250);
			((FilterConstraintContainerArgsContext)_localctx).filterConstraint = filterConstraint();
			((FilterConstraintContainerArgsContext)_localctx).constraints.add(((FilterConstraintContainerArgsContext)_localctx).filterConstraint);
			setState(255);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ARGS_DELIMITER) {
				{
				{
				setState(251);
				match(ARGS_DELIMITER);
				setState(252);
				((FilterConstraintContainerArgsContext)_localctx).filterConstraint = filterConstraint();
				((FilterConstraintContainerArgsContext)_localctx).constraints.add(((FilterConstraintContainerArgsContext)_localctx).filterConstraint);
				}
				}
				setState(257);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(258);
			match(ARGS_CLOSING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class OrderConstraintContainerArgsContext extends ParserRuleContext {
		public OrderConstraintContext orderConstraint;
		public List<OrderConstraintContext> constraints = new ArrayList<OrderConstraintContext>();
		public TerminalNode ARGS_OPENING() { return getToken(EvitaQLParser.ARGS_OPENING, 0); }
		public TerminalNode ARGS_CLOSING() { return getToken(EvitaQLParser.ARGS_CLOSING, 0); }
		public List<OrderConstraintContext> orderConstraint() {
			return getRuleContexts(OrderConstraintContext.class);
		}
		public OrderConstraintContext orderConstraint(int i) {
			return getRuleContext(OrderConstraintContext.class,i);
		}
		public List<TerminalNode> ARGS_DELIMITER() { return getTokens(EvitaQLParser.ARGS_DELIMITER); }
		public TerminalNode ARGS_DELIMITER(int i) {
			return getToken(EvitaQLParser.ARGS_DELIMITER, i);
		}
		public OrderConstraintContainerArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_orderConstraintContainerArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterOrderConstraintContainerArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitOrderConstraintContainerArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitOrderConstraintContainerArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OrderConstraintContainerArgsContext orderConstraintContainerArgs() throws RecognitionException {
		OrderConstraintContainerArgsContext _localctx = new OrderConstraintContainerArgsContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_orderConstraintContainerArgs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(260);
			match(ARGS_OPENING);
			setState(261);
			((OrderConstraintContainerArgsContext)_localctx).orderConstraint = orderConstraint();
			((OrderConstraintContainerArgsContext)_localctx).constraints.add(((OrderConstraintContainerArgsContext)_localctx).orderConstraint);
			setState(266);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ARGS_DELIMITER) {
				{
				{
				setState(262);
				match(ARGS_DELIMITER);
				setState(263);
				((OrderConstraintContainerArgsContext)_localctx).orderConstraint = orderConstraint();
				((OrderConstraintContainerArgsContext)_localctx).constraints.add(((OrderConstraintContainerArgsContext)_localctx).orderConstraint);
				}
				}
				setState(268);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(269);
			match(ARGS_CLOSING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class RequireConstraintContainerArgsContext extends ParserRuleContext {
		public RequireConstraintContext requireConstraint;
		public List<RequireConstraintContext> constraints = new ArrayList<RequireConstraintContext>();
		public TerminalNode ARGS_OPENING() { return getToken(EvitaQLParser.ARGS_OPENING, 0); }
		public TerminalNode ARGS_CLOSING() { return getToken(EvitaQLParser.ARGS_CLOSING, 0); }
		public List<RequireConstraintContext> requireConstraint() {
			return getRuleContexts(RequireConstraintContext.class);
		}
		public RequireConstraintContext requireConstraint(int i) {
			return getRuleContext(RequireConstraintContext.class,i);
		}
		public List<TerminalNode> ARGS_DELIMITER() { return getTokens(EvitaQLParser.ARGS_DELIMITER); }
		public TerminalNode ARGS_DELIMITER(int i) {
			return getToken(EvitaQLParser.ARGS_DELIMITER, i);
		}
		public RequireConstraintContainerArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_requireConstraintContainerArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterRequireConstraintContainerArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitRequireConstraintContainerArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitRequireConstraintContainerArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final RequireConstraintContainerArgsContext requireConstraintContainerArgs() throws RecognitionException {
		RequireConstraintContainerArgsContext _localctx = new RequireConstraintContainerArgsContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_requireConstraintContainerArgs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(271);
			match(ARGS_OPENING);
			setState(272);
			((RequireConstraintContainerArgsContext)_localctx).requireConstraint = requireConstraint();
			((RequireConstraintContainerArgsContext)_localctx).constraints.add(((RequireConstraintContainerArgsContext)_localctx).requireConstraint);
			setState(277);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ARGS_DELIMITER) {
				{
				{
				setState(273);
				match(ARGS_DELIMITER);
				setState(274);
				((RequireConstraintContainerArgsContext)_localctx).requireConstraint = requireConstraint();
				((RequireConstraintContainerArgsContext)_localctx).constraints.add(((RequireConstraintContainerArgsContext)_localctx).requireConstraint);
				}
				}
				setState(279);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(280);
			match(ARGS_CLOSING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NameArgsContext extends ParserRuleContext {
		public IdentifierContext name;
		public TerminalNode ARGS_OPENING() { return getToken(EvitaQLParser.ARGS_OPENING, 0); }
		public TerminalNode ARGS_CLOSING() { return getToken(EvitaQLParser.ARGS_CLOSING, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public NameArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nameArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterNameArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitNameArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitNameArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NameArgsContext nameArgs() throws RecognitionException {
		NameArgsContext _localctx = new NameArgsContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_nameArgs);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(282);
			match(ARGS_OPENING);
			setState(283);
			((NameArgsContext)_localctx).name = identifier();
			setState(284);
			match(ARGS_CLOSING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NameWithValueArgsContext extends ParserRuleContext {
		public IdentifierContext name;
		public LiteralContext value;
		public TerminalNode ARGS_OPENING() { return getToken(EvitaQLParser.ARGS_OPENING, 0); }
		public TerminalNode ARGS_DELIMITER() { return getToken(EvitaQLParser.ARGS_DELIMITER, 0); }
		public TerminalNode ARGS_CLOSING() { return getToken(EvitaQLParser.ARGS_CLOSING, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public NameWithValueArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nameWithValueArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterNameWithValueArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitNameWithValueArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitNameWithValueArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NameWithValueArgsContext nameWithValueArgs() throws RecognitionException {
		NameWithValueArgsContext _localctx = new NameWithValueArgsContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_nameWithValueArgs);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(286);
			match(ARGS_OPENING);
			setState(287);
			((NameWithValueArgsContext)_localctx).name = identifier();
			setState(288);
			match(ARGS_DELIMITER);
			setState(289);
			((NameWithValueArgsContext)_localctx).value = literal();
			setState(290);
			match(ARGS_CLOSING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NameWithValueListArgsContext extends ParserRuleContext {
		public IdentifierContext name;
		public LiteralContext literal;
		public List<LiteralContext> values = new ArrayList<LiteralContext>();
		public TerminalNode ARGS_OPENING() { return getToken(EvitaQLParser.ARGS_OPENING, 0); }
		public TerminalNode ARGS_CLOSING() { return getToken(EvitaQLParser.ARGS_CLOSING, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public List<TerminalNode> ARGS_DELIMITER() { return getTokens(EvitaQLParser.ARGS_DELIMITER); }
		public TerminalNode ARGS_DELIMITER(int i) {
			return getToken(EvitaQLParser.ARGS_DELIMITER, i);
		}
		public List<LiteralContext> literal() {
			return getRuleContexts(LiteralContext.class);
		}
		public LiteralContext literal(int i) {
			return getRuleContext(LiteralContext.class,i);
		}
		public NameWithValueListArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nameWithValueListArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterNameWithValueListArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitNameWithValueListArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitNameWithValueListArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NameWithValueListArgsContext nameWithValueListArgs() throws RecognitionException {
		NameWithValueListArgsContext _localctx = new NameWithValueListArgsContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_nameWithValueListArgs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(292);
			match(ARGS_OPENING);
			setState(293);
			((NameWithValueListArgsContext)_localctx).name = identifier();
			setState(296); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(294);
				match(ARGS_DELIMITER);
				setState(295);
				((NameWithValueListArgsContext)_localctx).literal = literal();
				((NameWithValueListArgsContext)_localctx).values.add(((NameWithValueListArgsContext)_localctx).literal);
				}
				}
				setState(298); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==ARGS_DELIMITER );
			setState(300);
			match(ARGS_CLOSING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NameWithBetweenValuesArgsContext extends ParserRuleContext {
		public IdentifierContext name;
		public LiteralContext valueFrom;
		public LiteralContext valueTo;
		public TerminalNode ARGS_OPENING() { return getToken(EvitaQLParser.ARGS_OPENING, 0); }
		public List<TerminalNode> ARGS_DELIMITER() { return getTokens(EvitaQLParser.ARGS_DELIMITER); }
		public TerminalNode ARGS_DELIMITER(int i) {
			return getToken(EvitaQLParser.ARGS_DELIMITER, i);
		}
		public TerminalNode ARGS_CLOSING() { return getToken(EvitaQLParser.ARGS_CLOSING, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public List<LiteralContext> literal() {
			return getRuleContexts(LiteralContext.class);
		}
		public LiteralContext literal(int i) {
			return getRuleContext(LiteralContext.class,i);
		}
		public NameWithBetweenValuesArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nameWithBetweenValuesArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterNameWithBetweenValuesArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitNameWithBetweenValuesArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitNameWithBetweenValuesArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NameWithBetweenValuesArgsContext nameWithBetweenValuesArgs() throws RecognitionException {
		NameWithBetweenValuesArgsContext _localctx = new NameWithBetweenValuesArgsContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_nameWithBetweenValuesArgs);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(302);
			match(ARGS_OPENING);
			setState(303);
			((NameWithBetweenValuesArgsContext)_localctx).name = identifier();
			setState(304);
			match(ARGS_DELIMITER);
			setState(305);
			((NameWithBetweenValuesArgsContext)_localctx).valueFrom = literal();
			setState(306);
			match(ARGS_DELIMITER);
			setState(307);
			((NameWithBetweenValuesArgsContext)_localctx).valueTo = literal();
			setState(308);
			match(ARGS_CLOSING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ValueArgsContext extends ParserRuleContext {
		public LiteralContext value;
		public TerminalNode ARGS_OPENING() { return getToken(EvitaQLParser.ARGS_OPENING, 0); }
		public TerminalNode ARGS_CLOSING() { return getToken(EvitaQLParser.ARGS_CLOSING, 0); }
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public ValueArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_valueArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterValueArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitValueArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitValueArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ValueArgsContext valueArgs() throws RecognitionException {
		ValueArgsContext _localctx = new ValueArgsContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_valueArgs);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(310);
			match(ARGS_OPENING);
			setState(311);
			((ValueArgsContext)_localctx).value = literal();
			setState(312);
			match(ARGS_CLOSING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ValueListArgsContext extends ParserRuleContext {
		public LiteralContext literal;
		public List<LiteralContext> values = new ArrayList<LiteralContext>();
		public TerminalNode ARGS_OPENING() { return getToken(EvitaQLParser.ARGS_OPENING, 0); }
		public TerminalNode ARGS_CLOSING() { return getToken(EvitaQLParser.ARGS_CLOSING, 0); }
		public List<LiteralContext> literal() {
			return getRuleContexts(LiteralContext.class);
		}
		public LiteralContext literal(int i) {
			return getRuleContext(LiteralContext.class,i);
		}
		public List<TerminalNode> ARGS_DELIMITER() { return getTokens(EvitaQLParser.ARGS_DELIMITER); }
		public TerminalNode ARGS_DELIMITER(int i) {
			return getToken(EvitaQLParser.ARGS_DELIMITER, i);
		}
		public ValueListArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_valueListArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterValueListArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitValueListArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitValueListArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ValueListArgsContext valueListArgs() throws RecognitionException {
		ValueListArgsContext _localctx = new ValueListArgsContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_valueListArgs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(314);
			match(ARGS_OPENING);
			setState(315);
			((ValueListArgsContext)_localctx).literal = literal();
			((ValueListArgsContext)_localctx).values.add(((ValueListArgsContext)_localctx).literal);
			setState(320);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ARGS_DELIMITER) {
				{
				{
				setState(316);
				match(ARGS_DELIMITER);
				setState(317);
				((ValueListArgsContext)_localctx).literal = literal();
				((ValueListArgsContext)_localctx).values.add(((ValueListArgsContext)_localctx).literal);
				}
				}
				setState(322);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(323);
			match(ARGS_CLOSING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class BetweenValuesArgsContext extends ParserRuleContext {
		public LiteralContext valueFrom;
		public LiteralContext valueTo;
		public TerminalNode ARGS_OPENING() { return getToken(EvitaQLParser.ARGS_OPENING, 0); }
		public TerminalNode ARGS_DELIMITER() { return getToken(EvitaQLParser.ARGS_DELIMITER, 0); }
		public TerminalNode ARGS_CLOSING() { return getToken(EvitaQLParser.ARGS_CLOSING, 0); }
		public List<LiteralContext> literal() {
			return getRuleContexts(LiteralContext.class);
		}
		public LiteralContext literal(int i) {
			return getRuleContext(LiteralContext.class,i);
		}
		public BetweenValuesArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_betweenValuesArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterBetweenValuesArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitBetweenValuesArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitBetweenValuesArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BetweenValuesArgsContext betweenValuesArgs() throws RecognitionException {
		BetweenValuesArgsContext _localctx = new BetweenValuesArgsContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_betweenValuesArgs);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(325);
			match(ARGS_OPENING);
			setState(326);
			((BetweenValuesArgsContext)_localctx).valueFrom = literal();
			setState(327);
			match(ARGS_DELIMITER);
			setState(328);
			((BetweenValuesArgsContext)_localctx).valueTo = literal();
			setState(329);
			match(ARGS_CLOSING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class NameListArgsContext extends ParserRuleContext {
		public IdentifierContext identifier;
		public List<IdentifierContext> names = new ArrayList<IdentifierContext>();
		public TerminalNode ARGS_OPENING() { return getToken(EvitaQLParser.ARGS_OPENING, 0); }
		public TerminalNode ARGS_CLOSING() { return getToken(EvitaQLParser.ARGS_CLOSING, 0); }
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public List<TerminalNode> ARGS_DELIMITER() { return getTokens(EvitaQLParser.ARGS_DELIMITER); }
		public TerminalNode ARGS_DELIMITER(int i) {
			return getToken(EvitaQLParser.ARGS_DELIMITER, i);
		}
		public NameListArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nameListArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterNameListArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitNameListArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitNameListArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NameListArgsContext nameListArgs() throws RecognitionException {
		NameListArgsContext _localctx = new NameListArgsContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_nameListArgs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(331);
			match(ARGS_OPENING);
			setState(332);
			((NameListArgsContext)_localctx).identifier = identifier();
			((NameListArgsContext)_localctx).names.add(((NameListArgsContext)_localctx).identifier);
			setState(337);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ARGS_DELIMITER) {
				{
				{
				setState(333);
				match(ARGS_DELIMITER);
				setState(334);
				((NameListArgsContext)_localctx).identifier = identifier();
				((NameListArgsContext)_localctx).names.add(((NameListArgsContext)_localctx).identifier);
				}
				}
				setState(339);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(340);
			match(ARGS_CLOSING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ValueWithNameListArgsContext extends ParserRuleContext {
		public LiteralContext value;
		public IdentifierContext identifier;
		public List<IdentifierContext> names = new ArrayList<IdentifierContext>();
		public TerminalNode ARGS_OPENING() { return getToken(EvitaQLParser.ARGS_OPENING, 0); }
		public TerminalNode ARGS_CLOSING() { return getToken(EvitaQLParser.ARGS_CLOSING, 0); }
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public List<TerminalNode> ARGS_DELIMITER() { return getTokens(EvitaQLParser.ARGS_DELIMITER); }
		public TerminalNode ARGS_DELIMITER(int i) {
			return getToken(EvitaQLParser.ARGS_DELIMITER, i);
		}
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public ValueWithNameListArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_valueWithNameListArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterValueWithNameListArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitValueWithNameListArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitValueWithNameListArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ValueWithNameListArgsContext valueWithNameListArgs() throws RecognitionException {
		ValueWithNameListArgsContext _localctx = new ValueWithNameListArgsContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_valueWithNameListArgs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(342);
			match(ARGS_OPENING);
			setState(343);
			((ValueWithNameListArgsContext)_localctx).value = literal();
			setState(346); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(344);
				match(ARGS_DELIMITER);
				setState(345);
				((ValueWithNameListArgsContext)_localctx).identifier = identifier();
				((ValueWithNameListArgsContext)_localctx).names.add(((ValueWithNameListArgsContext)_localctx).identifier);
				}
				}
				setState(348); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==ARGS_DELIMITER );
			setState(350);
			match(ARGS_CLOSING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ReferencedTypesArgsContext extends ParserRuleContext {
		public LiteralContext literal;
		public List<LiteralContext> referencedTypes = new ArrayList<LiteralContext>();
		public TerminalNode ARGS_OPENING() { return getToken(EvitaQLParser.ARGS_OPENING, 0); }
		public TerminalNode ARGS_CLOSING() { return getToken(EvitaQLParser.ARGS_CLOSING, 0); }
		public List<LiteralContext> literal() {
			return getRuleContexts(LiteralContext.class);
		}
		public LiteralContext literal(int i) {
			return getRuleContext(LiteralContext.class,i);
		}
		public List<TerminalNode> ARGS_DELIMITER() { return getTokens(EvitaQLParser.ARGS_DELIMITER); }
		public TerminalNode ARGS_DELIMITER(int i) {
			return getToken(EvitaQLParser.ARGS_DELIMITER, i);
		}
		public ReferencedTypesArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_referencedTypesArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterReferencedTypesArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitReferencedTypesArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitReferencedTypesArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ReferencedTypesArgsContext referencedTypesArgs() throws RecognitionException {
		ReferencedTypesArgsContext _localctx = new ReferencedTypesArgsContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_referencedTypesArgs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(352);
			match(ARGS_OPENING);
			setState(353);
			((ReferencedTypesArgsContext)_localctx).literal = literal();
			((ReferencedTypesArgsContext)_localctx).referencedTypes.add(((ReferencedTypesArgsContext)_localctx).literal);
			setState(358);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ARGS_DELIMITER) {
				{
				{
				setState(354);
				match(ARGS_DELIMITER);
				setState(355);
				((ReferencedTypesArgsContext)_localctx).literal = literal();
				((ReferencedTypesArgsContext)_localctx).referencedTypes.add(((ReferencedTypesArgsContext)_localctx).literal);
				}
				}
				setState(360);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(361);
			match(ARGS_CLOSING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class EntityTypeArgsContext extends ParserRuleContext {
		public LiteralContext entityType;
		public TerminalNode ARGS_OPENING() { return getToken(EvitaQLParser.ARGS_OPENING, 0); }
		public TerminalNode ARGS_CLOSING() { return getToken(EvitaQLParser.ARGS_CLOSING, 0); }
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public EntityTypeArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_entityTypeArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterEntityTypeArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitEntityTypeArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitEntityTypeArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EntityTypeArgsContext entityTypeArgs() throws RecognitionException {
		EntityTypeArgsContext _localctx = new EntityTypeArgsContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_entityTypeArgs);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(363);
			match(ARGS_OPENING);
			setState(364);
			((EntityTypeArgsContext)_localctx).entityType = literal();
			setState(365);
			match(ARGS_CLOSING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class EntityTypeListArgsContext extends ParserRuleContext {
		public LiteralContext literal;
		public List<LiteralContext> entityTypes = new ArrayList<LiteralContext>();
		public TerminalNode ARGS_OPENING() { return getToken(EvitaQLParser.ARGS_OPENING, 0); }
		public TerminalNode ARGS_CLOSING() { return getToken(EvitaQLParser.ARGS_CLOSING, 0); }
		public List<LiteralContext> literal() {
			return getRuleContexts(LiteralContext.class);
		}
		public LiteralContext literal(int i) {
			return getRuleContext(LiteralContext.class,i);
		}
		public List<TerminalNode> ARGS_DELIMITER() { return getTokens(EvitaQLParser.ARGS_DELIMITER); }
		public TerminalNode ARGS_DELIMITER(int i) {
			return getToken(EvitaQLParser.ARGS_DELIMITER, i);
		}
		public EntityTypeListArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_entityTypeListArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterEntityTypeListArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitEntityTypeListArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitEntityTypeListArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EntityTypeListArgsContext entityTypeListArgs() throws RecognitionException {
		EntityTypeListArgsContext _localctx = new EntityTypeListArgsContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_entityTypeListArgs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(367);
			match(ARGS_OPENING);
			setState(368);
			((EntityTypeListArgsContext)_localctx).literal = literal();
			((EntityTypeListArgsContext)_localctx).entityTypes.add(((EntityTypeListArgsContext)_localctx).literal);
			setState(373);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ARGS_DELIMITER) {
				{
				{
				setState(369);
				match(ARGS_DELIMITER);
				setState(370);
				((EntityTypeListArgsContext)_localctx).literal = literal();
				((EntityTypeListArgsContext)_localctx).entityTypes.add(((EntityTypeListArgsContext)_localctx).literal);
				}
				}
				setState(375);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(376);
			match(ARGS_CLOSING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class EntityTypeWithValueListArgsContext extends ParserRuleContext {
		public LiteralContext entityType;
		public LiteralContext literal;
		public List<LiteralContext> values = new ArrayList<LiteralContext>();
		public TerminalNode ARGS_OPENING() { return getToken(EvitaQLParser.ARGS_OPENING, 0); }
		public TerminalNode ARGS_CLOSING() { return getToken(EvitaQLParser.ARGS_CLOSING, 0); }
		public List<LiteralContext> literal() {
			return getRuleContexts(LiteralContext.class);
		}
		public LiteralContext literal(int i) {
			return getRuleContext(LiteralContext.class,i);
		}
		public List<TerminalNode> ARGS_DELIMITER() { return getTokens(EvitaQLParser.ARGS_DELIMITER); }
		public TerminalNode ARGS_DELIMITER(int i) {
			return getToken(EvitaQLParser.ARGS_DELIMITER, i);
		}
		public EntityTypeWithValueListArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_entityTypeWithValueListArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterEntityTypeWithValueListArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitEntityTypeWithValueListArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitEntityTypeWithValueListArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EntityTypeWithValueListArgsContext entityTypeWithValueListArgs() throws RecognitionException {
		EntityTypeWithValueListArgsContext _localctx = new EntityTypeWithValueListArgsContext(_ctx, getState());
		enterRule(_localctx, 48, RULE_entityTypeWithValueListArgs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(378);
			match(ARGS_OPENING);
			setState(379);
			((EntityTypeWithValueListArgsContext)_localctx).entityType = literal();
			setState(382); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(380);
				match(ARGS_DELIMITER);
				setState(381);
				((EntityTypeWithValueListArgsContext)_localctx).literal = literal();
				((EntityTypeWithValueListArgsContext)_localctx).values.add(((EntityTypeWithValueListArgsContext)_localctx).literal);
				}
				}
				setState(384); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==ARGS_DELIMITER );
			setState(386);
			match(ARGS_CLOSING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class EntityTypeWithFilterConstraintArgsContext extends ParserRuleContext {
		public LiteralContext entityType;
		public TerminalNode ARGS_OPENING() { return getToken(EvitaQLParser.ARGS_OPENING, 0); }
		public TerminalNode ARGS_DELIMITER() { return getToken(EvitaQLParser.ARGS_DELIMITER, 0); }
		public FilterConstraintContext filterConstraint() {
			return getRuleContext(FilterConstraintContext.class,0);
		}
		public TerminalNode ARGS_CLOSING() { return getToken(EvitaQLParser.ARGS_CLOSING, 0); }
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public EntityTypeWithFilterConstraintArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_entityTypeWithFilterConstraintArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterEntityTypeWithFilterConstraintArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitEntityTypeWithFilterConstraintArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitEntityTypeWithFilterConstraintArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EntityTypeWithFilterConstraintArgsContext entityTypeWithFilterConstraintArgs() throws RecognitionException {
		EntityTypeWithFilterConstraintArgsContext _localctx = new EntityTypeWithFilterConstraintArgsContext(_ctx, getState());
		enterRule(_localctx, 50, RULE_entityTypeWithFilterConstraintArgs);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(388);
			match(ARGS_OPENING);
			setState(389);
			((EntityTypeWithFilterConstraintArgsContext)_localctx).entityType = literal();
			setState(390);
			match(ARGS_DELIMITER);
			setState(391);
			filterConstraint();
			setState(392);
			match(ARGS_CLOSING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class EntityTypeWithOrderConstraintListArgsContext extends ParserRuleContext {
		public LiteralContext entityType;
		public OrderConstraintContext orderConstraint;
		public List<OrderConstraintContext> constrains = new ArrayList<OrderConstraintContext>();
		public TerminalNode ARGS_OPENING() { return getToken(EvitaQLParser.ARGS_OPENING, 0); }
		public TerminalNode ARGS_CLOSING() { return getToken(EvitaQLParser.ARGS_CLOSING, 0); }
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public List<TerminalNode> ARGS_DELIMITER() { return getTokens(EvitaQLParser.ARGS_DELIMITER); }
		public TerminalNode ARGS_DELIMITER(int i) {
			return getToken(EvitaQLParser.ARGS_DELIMITER, i);
		}
		public List<OrderConstraintContext> orderConstraint() {
			return getRuleContexts(OrderConstraintContext.class);
		}
		public OrderConstraintContext orderConstraint(int i) {
			return getRuleContext(OrderConstraintContext.class,i);
		}
		public EntityTypeWithOrderConstraintListArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_entityTypeWithOrderConstraintListArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterEntityTypeWithOrderConstraintListArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitEntityTypeWithOrderConstraintListArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitEntityTypeWithOrderConstraintListArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EntityTypeWithOrderConstraintListArgsContext entityTypeWithOrderConstraintListArgs() throws RecognitionException {
		EntityTypeWithOrderConstraintListArgsContext _localctx = new EntityTypeWithOrderConstraintListArgsContext(_ctx, getState());
		enterRule(_localctx, 52, RULE_entityTypeWithOrderConstraintListArgs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(394);
			match(ARGS_OPENING);
			setState(395);
			((EntityTypeWithOrderConstraintListArgsContext)_localctx).entityType = literal();
			setState(398); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(396);
				match(ARGS_DELIMITER);
				setState(397);
				((EntityTypeWithOrderConstraintListArgsContext)_localctx).orderConstraint = orderConstraint();
				((EntityTypeWithOrderConstraintListArgsContext)_localctx).constrains.add(((EntityTypeWithOrderConstraintListArgsContext)_localctx).orderConstraint);
				}
				}
				setState(400); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==ARGS_DELIMITER );
			setState(402);
			match(ARGS_CLOSING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class EntityTypeWithRequireConstraintListArgsContext extends ParserRuleContext {
		public LiteralContext entityType;
		public RequireConstraintContext requireConstraint;
		public List<RequireConstraintContext> constrains = new ArrayList<RequireConstraintContext>();
		public TerminalNode ARGS_OPENING() { return getToken(EvitaQLParser.ARGS_OPENING, 0); }
		public TerminalNode ARGS_CLOSING() { return getToken(EvitaQLParser.ARGS_CLOSING, 0); }
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public List<TerminalNode> ARGS_DELIMITER() { return getTokens(EvitaQLParser.ARGS_DELIMITER); }
		public TerminalNode ARGS_DELIMITER(int i) {
			return getToken(EvitaQLParser.ARGS_DELIMITER, i);
		}
		public List<RequireConstraintContext> requireConstraint() {
			return getRuleContexts(RequireConstraintContext.class);
		}
		public RequireConstraintContext requireConstraint(int i) {
			return getRuleContext(RequireConstraintContext.class,i);
		}
		public EntityTypeWithRequireConstraintListArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_entityTypeWithRequireConstraintListArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterEntityTypeWithRequireConstraintListArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitEntityTypeWithRequireConstraintListArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitEntityTypeWithRequireConstraintListArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EntityTypeWithRequireConstraintListArgsContext entityTypeWithRequireConstraintListArgs() throws RecognitionException {
		EntityTypeWithRequireConstraintListArgsContext _localctx = new EntityTypeWithRequireConstraintListArgsContext(_ctx, getState());
		enterRule(_localctx, 54, RULE_entityTypeWithRequireConstraintListArgs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(404);
			match(ARGS_OPENING);
			setState(405);
			((EntityTypeWithRequireConstraintListArgsContext)_localctx).entityType = literal();
			setState(410);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ARGS_DELIMITER) {
				{
				{
				setState(406);
				match(ARGS_DELIMITER);
				setState(407);
				((EntityTypeWithRequireConstraintListArgsContext)_localctx).requireConstraint = requireConstraint();
				((EntityTypeWithRequireConstraintListArgsContext)_localctx).constrains.add(((EntityTypeWithRequireConstraintListArgsContext)_localctx).requireConstraint);
				}
				}
				setState(412);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(413);
			match(ARGS_CLOSING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class WithinHierarchyConstraintArgsContext extends ParserRuleContext {
		public LiteralContext entityType;
		public LiteralContext primaryKey;
		public FilterConstraintContext filterConstraint;
		public List<FilterConstraintContext> constrains = new ArrayList<FilterConstraintContext>();
		public TerminalNode ARGS_OPENING() { return getToken(EvitaQLParser.ARGS_OPENING, 0); }
		public TerminalNode ARGS_CLOSING() { return getToken(EvitaQLParser.ARGS_CLOSING, 0); }
		public List<LiteralContext> literal() {
			return getRuleContexts(LiteralContext.class);
		}
		public LiteralContext literal(int i) {
			return getRuleContext(LiteralContext.class,i);
		}
		public List<TerminalNode> ARGS_DELIMITER() { return getTokens(EvitaQLParser.ARGS_DELIMITER); }
		public TerminalNode ARGS_DELIMITER(int i) {
			return getToken(EvitaQLParser.ARGS_DELIMITER, i);
		}
		public List<FilterConstraintContext> filterConstraint() {
			return getRuleContexts(FilterConstraintContext.class);
		}
		public FilterConstraintContext filterConstraint(int i) {
			return getRuleContext(FilterConstraintContext.class,i);
		}
		public WithinHierarchyConstraintArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_withinHierarchyConstraintArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterWithinHierarchyConstraintArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitWithinHierarchyConstraintArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitWithinHierarchyConstraintArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WithinHierarchyConstraintArgsContext withinHierarchyConstraintArgs() throws RecognitionException {
		WithinHierarchyConstraintArgsContext _localctx = new WithinHierarchyConstraintArgsContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_withinHierarchyConstraintArgs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(415);
			match(ARGS_OPENING);
			setState(419);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,24,_ctx) ) {
			case 1:
				{
				setState(416);
				((WithinHierarchyConstraintArgsContext)_localctx).entityType = literal();
				setState(417);
				match(ARGS_DELIMITER);
				}
				break;
			}
			setState(421);
			((WithinHierarchyConstraintArgsContext)_localctx).primaryKey = literal();
			setState(426);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ARGS_DELIMITER) {
				{
				{
				setState(422);
				match(ARGS_DELIMITER);
				setState(423);
				((WithinHierarchyConstraintArgsContext)_localctx).filterConstraint = filterConstraint();
				((WithinHierarchyConstraintArgsContext)_localctx).constrains.add(((WithinHierarchyConstraintArgsContext)_localctx).filterConstraint);
				}
				}
				setState(428);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(429);
			match(ARGS_CLOSING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class WithinRootHierarchyConstraintArgsContext extends ParserRuleContext {
		public LiteralContext entityType;
		public FilterConstraintContext filterConstraint;
		public List<FilterConstraintContext> constrains = new ArrayList<FilterConstraintContext>();
		public TerminalNode ARGS_OPENING() { return getToken(EvitaQLParser.ARGS_OPENING, 0); }
		public TerminalNode ARGS_CLOSING() { return getToken(EvitaQLParser.ARGS_CLOSING, 0); }
		public LiteralContext literal() {
			return getRuleContext(LiteralContext.class,0);
		}
		public List<TerminalNode> ARGS_DELIMITER() { return getTokens(EvitaQLParser.ARGS_DELIMITER); }
		public TerminalNode ARGS_DELIMITER(int i) {
			return getToken(EvitaQLParser.ARGS_DELIMITER, i);
		}
		public List<FilterConstraintContext> filterConstraint() {
			return getRuleContexts(FilterConstraintContext.class);
		}
		public FilterConstraintContext filterConstraint(int i) {
			return getRuleContext(FilterConstraintContext.class,i);
		}
		public WithinRootHierarchyConstraintArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_withinRootHierarchyConstraintArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterWithinRootHierarchyConstraintArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitWithinRootHierarchyConstraintArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitWithinRootHierarchyConstraintArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final WithinRootHierarchyConstraintArgsContext withinRootHierarchyConstraintArgs() throws RecognitionException {
		WithinRootHierarchyConstraintArgsContext _localctx = new WithinRootHierarchyConstraintArgsContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_withinRootHierarchyConstraintArgs);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(431);
			match(ARGS_OPENING);
			setState(448);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,29,_ctx) ) {
			case 1:
				{
				setState(432);
				((WithinRootHierarchyConstraintArgsContext)_localctx).entityType = literal();
				}
				break;
			case 2:
				{
				{
				setState(436);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (((((_la - 62)) & ~0x3f) == 0 && ((1L << (_la - 62)) & ((1L << (STRING - 62)) | (1L << (INT - 62)) | (1L << (FLOAT - 62)) | (1L << (BOOLEAN - 62)) | (1L << (DATE - 62)) | (1L << (TIME - 62)) | (1L << (DATE_TIME - 62)) | (1L << (ZONED_DATE_TIME - 62)) | (1L << (NUMBER_RANGE - 62)) | (1L << (DATE_TIME_RANGE - 62)) | (1L << (ENUM - 62)) | (1L << (LOCALE - 62)) | (1L << (MULTIPLE_OPENING - 62)))) != 0)) {
					{
					setState(433);
					((WithinRootHierarchyConstraintArgsContext)_localctx).entityType = literal();
					setState(434);
					match(ARGS_DELIMITER);
					}
				}

				setState(446);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__2) | (1L << T__3) | (1L << T__4) | (1L << T__5) | (1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__21) | (1L << T__22) | (1L << T__23) | (1L << T__24) | (1L << T__25) | (1L << T__26) | (1L << T__27) | (1L << T__28) | (1L << T__29) | (1L << T__30) | (1L << T__31) | (1L << T__32) | (1L << T__33) | (1L << T__34))) != 0)) {
					{
					setState(438);
					((WithinRootHierarchyConstraintArgsContext)_localctx).filterConstraint = filterConstraint();
					((WithinRootHierarchyConstraintArgsContext)_localctx).constrains.add(((WithinRootHierarchyConstraintArgsContext)_localctx).filterConstraint);
					setState(443);
					_errHandler.sync(this);
					_la = _input.LA(1);
					while (_la==ARGS_DELIMITER) {
						{
						{
						setState(439);
						match(ARGS_DELIMITER);
						setState(440);
						((WithinRootHierarchyConstraintArgsContext)_localctx).filterConstraint = filterConstraint();
						((WithinRootHierarchyConstraintArgsContext)_localctx).constrains.add(((WithinRootHierarchyConstraintArgsContext)_localctx).filterConstraint);
						}
						}
						setState(445);
						_errHandler.sync(this);
						_la = _input.LA(1);
					}
					}
				}

				}
				}
				break;
			}
			setState(450);
			match(ARGS_CLOSING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class PageConstraintArgsContext extends ParserRuleContext {
		public LiteralContext pageNumber;
		public LiteralContext pageSize;
		public TerminalNode ARGS_OPENING() { return getToken(EvitaQLParser.ARGS_OPENING, 0); }
		public TerminalNode ARGS_DELIMITER() { return getToken(EvitaQLParser.ARGS_DELIMITER, 0); }
		public TerminalNode ARGS_CLOSING() { return getToken(EvitaQLParser.ARGS_CLOSING, 0); }
		public List<LiteralContext> literal() {
			return getRuleContexts(LiteralContext.class);
		}
		public LiteralContext literal(int i) {
			return getRuleContext(LiteralContext.class,i);
		}
		public PageConstraintArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_pageConstraintArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterPageConstraintArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitPageConstraintArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitPageConstraintArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final PageConstraintArgsContext pageConstraintArgs() throws RecognitionException {
		PageConstraintArgsContext _localctx = new PageConstraintArgsContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_pageConstraintArgs);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(452);
			match(ARGS_OPENING);
			setState(453);
			((PageConstraintArgsContext)_localctx).pageNumber = literal();
			setState(454);
			match(ARGS_DELIMITER);
			setState(455);
			((PageConstraintArgsContext)_localctx).pageSize = literal();
			setState(456);
			match(ARGS_CLOSING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class StripConstraintArgsContext extends ParserRuleContext {
		public LiteralContext offset;
		public LiteralContext limit;
		public TerminalNode ARGS_OPENING() { return getToken(EvitaQLParser.ARGS_OPENING, 0); }
		public TerminalNode ARGS_DELIMITER() { return getToken(EvitaQLParser.ARGS_DELIMITER, 0); }
		public TerminalNode ARGS_CLOSING() { return getToken(EvitaQLParser.ARGS_CLOSING, 0); }
		public List<LiteralContext> literal() {
			return getRuleContexts(LiteralContext.class);
		}
		public LiteralContext literal(int i) {
			return getRuleContext(LiteralContext.class,i);
		}
		public StripConstraintArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stripConstraintArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterStripConstraintArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitStripConstraintArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitStripConstraintArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StripConstraintArgsContext stripConstraintArgs() throws RecognitionException {
		StripConstraintArgsContext _localctx = new StripConstraintArgsContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_stripConstraintArgs);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(458);
			match(ARGS_OPENING);
			setState(459);
			((StripConstraintArgsContext)_localctx).offset = literal();
			setState(460);
			match(ARGS_DELIMITER);
			setState(461);
			((StripConstraintArgsContext)_localctx).limit = literal();
			setState(462);
			match(ARGS_CLOSING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ParentsOfTypeConstraintArgsContext extends ParserRuleContext {
		public LiteralContext literal;
		public List<LiteralContext> entityTypes = new ArrayList<LiteralContext>();
		public RequireConstraintContext requireConstraint;
		public List<RequireConstraintContext> requireConstraints = new ArrayList<RequireConstraintContext>();
		public TerminalNode ARGS_OPENING() { return getToken(EvitaQLParser.ARGS_OPENING, 0); }
		public TerminalNode ARGS_CLOSING() { return getToken(EvitaQLParser.ARGS_CLOSING, 0); }
		public List<LiteralContext> literal() {
			return getRuleContexts(LiteralContext.class);
		}
		public LiteralContext literal(int i) {
			return getRuleContext(LiteralContext.class,i);
		}
		public List<TerminalNode> ARGS_DELIMITER() { return getTokens(EvitaQLParser.ARGS_DELIMITER); }
		public TerminalNode ARGS_DELIMITER(int i) {
			return getToken(EvitaQLParser.ARGS_DELIMITER, i);
		}
		public List<RequireConstraintContext> requireConstraint() {
			return getRuleContexts(RequireConstraintContext.class);
		}
		public RequireConstraintContext requireConstraint(int i) {
			return getRuleContext(RequireConstraintContext.class,i);
		}
		public ParentsOfTypeConstraintArgsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_parentsOfTypeConstraintArgs; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterParentsOfTypeConstraintArgs(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitParentsOfTypeConstraintArgs(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitParentsOfTypeConstraintArgs(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ParentsOfTypeConstraintArgsContext parentsOfTypeConstraintArgs() throws RecognitionException {
		ParentsOfTypeConstraintArgsContext _localctx = new ParentsOfTypeConstraintArgsContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_parentsOfTypeConstraintArgs);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(464);
			match(ARGS_OPENING);
			setState(465);
			((ParentsOfTypeConstraintArgsContext)_localctx).literal = literal();
			((ParentsOfTypeConstraintArgsContext)_localctx).entityTypes.add(((ParentsOfTypeConstraintArgsContext)_localctx).literal);
			setState(470);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,30,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(466);
					match(ARGS_DELIMITER);
					setState(467);
					((ParentsOfTypeConstraintArgsContext)_localctx).literal = literal();
					((ParentsOfTypeConstraintArgsContext)_localctx).entityTypes.add(((ParentsOfTypeConstraintArgsContext)_localctx).literal);
					}
					} 
				}
				setState(472);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,30,_ctx);
			}
			setState(477);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==ARGS_DELIMITER) {
				{
				{
				setState(473);
				match(ARGS_DELIMITER);
				setState(474);
				((ParentsOfTypeConstraintArgsContext)_localctx).requireConstraint = requireConstraint();
				((ParentsOfTypeConstraintArgsContext)_localctx).requireConstraints.add(((ParentsOfTypeConstraintArgsContext)_localctx).requireConstraint);
				}
				}
				setState(479);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(480);
			match(ARGS_CLOSING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class IdentifierContext extends ParserRuleContext {
		public TerminalNode STRING() { return getToken(EvitaQLParser.STRING, 0); }
		public IdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_identifier; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterIdentifier(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitIdentifier(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitIdentifier(this);
			else return visitor.visitChildren(this);
		}
	}

	public final IdentifierContext identifier() throws RecognitionException {
		IdentifierContext _localctx = new IdentifierContext(_ctx, getState());
		enterRule(_localctx, 66, RULE_identifier);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(482);
			match(STRING);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class LiteralContext extends ParserRuleContext {
		public LiteralContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_literal; }
	 
		public LiteralContext() { }
		public void copyFrom(LiteralContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class NumberRangeLiteralContext extends LiteralContext {
		public TerminalNode NUMBER_RANGE() { return getToken(EvitaQLParser.NUMBER_RANGE, 0); }
		public NumberRangeLiteralContext(LiteralContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterNumberRangeLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitNumberRangeLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitNumberRangeLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DateTimeLiteralContext extends LiteralContext {
		public TerminalNode DATE_TIME() { return getToken(EvitaQLParser.DATE_TIME, 0); }
		public DateTimeLiteralContext(LiteralContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterDateTimeLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitDateTimeLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitDateTimeLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class EnumLiteralContext extends LiteralContext {
		public TerminalNode ENUM() { return getToken(EvitaQLParser.ENUM, 0); }
		public EnumLiteralContext(LiteralContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterEnumLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitEnumLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitEnumLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class IntLiteralContext extends LiteralContext {
		public TerminalNode INT() { return getToken(EvitaQLParser.INT, 0); }
		public IntLiteralContext(LiteralContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterIntLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitIntLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitIntLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class ZonedDateTimeLiteralContext extends LiteralContext {
		public TerminalNode ZONED_DATE_TIME() { return getToken(EvitaQLParser.ZONED_DATE_TIME, 0); }
		public ZonedDateTimeLiteralContext(LiteralContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterZonedDateTimeLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitZonedDateTimeLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitZonedDateTimeLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class TimeLiteralContext extends LiteralContext {
		public TerminalNode TIME() { return getToken(EvitaQLParser.TIME, 0); }
		public TimeLiteralContext(LiteralContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterTimeLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitTimeLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitTimeLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class MultipleLiteralContext extends LiteralContext {
		public LiteralContext literal;
		public List<LiteralContext> values = new ArrayList<LiteralContext>();
		public TerminalNode MULTIPLE_OPENING() { return getToken(EvitaQLParser.MULTIPLE_OPENING, 0); }
		public TerminalNode MULTIPLE_CLOSING() { return getToken(EvitaQLParser.MULTIPLE_CLOSING, 0); }
		public List<LiteralContext> literal() {
			return getRuleContexts(LiteralContext.class);
		}
		public LiteralContext literal(int i) {
			return getRuleContext(LiteralContext.class,i);
		}
		public List<TerminalNode> ARGS_DELIMITER() { return getTokens(EvitaQLParser.ARGS_DELIMITER); }
		public TerminalNode ARGS_DELIMITER(int i) {
			return getToken(EvitaQLParser.ARGS_DELIMITER, i);
		}
		public MultipleLiteralContext(LiteralContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterMultipleLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitMultipleLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitMultipleLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class StringLiteralContext extends LiteralContext {
		public TerminalNode STRING() { return getToken(EvitaQLParser.STRING, 0); }
		public StringLiteralContext(LiteralContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterStringLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitStringLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitStringLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class LocaleLiteralContext extends LiteralContext {
		public TerminalNode LOCALE() { return getToken(EvitaQLParser.LOCALE, 0); }
		public LocaleLiteralContext(LiteralContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterLocaleLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitLocaleLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitLocaleLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class FloatLiteralContext extends LiteralContext {
		public TerminalNode FLOAT() { return getToken(EvitaQLParser.FLOAT, 0); }
		public FloatLiteralContext(LiteralContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterFloatLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitFloatLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitFloatLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DateLiteralContext extends LiteralContext {
		public TerminalNode DATE() { return getToken(EvitaQLParser.DATE, 0); }
		public DateLiteralContext(LiteralContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterDateLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitDateLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitDateLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DateTimeRangeLiteralContext extends LiteralContext {
		public TerminalNode DATE_TIME_RANGE() { return getToken(EvitaQLParser.DATE_TIME_RANGE, 0); }
		public DateTimeRangeLiteralContext(LiteralContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterDateTimeRangeLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitDateTimeRangeLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitDateTimeRangeLiteral(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class BooleanLiteralContext extends LiteralContext {
		public TerminalNode BOOLEAN() { return getToken(EvitaQLParser.BOOLEAN, 0); }
		public BooleanLiteralContext(LiteralContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).enterBooleanLiteral(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof EvitaQLListener ) ((EvitaQLListener)listener).exitBooleanLiteral(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof EvitaQLVisitor ) return ((EvitaQLVisitor<? extends T>)visitor).visitBooleanLiteral(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LiteralContext literal() throws RecognitionException {
		LiteralContext _localctx = new LiteralContext(_ctx, getState());
		enterRule(_localctx, 68, RULE_literal);
		int _la;
		try {
			setState(506);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case STRING:
				_localctx = new StringLiteralContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(484);
				match(STRING);
				}
				break;
			case INT:
				_localctx = new IntLiteralContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(485);
				match(INT);
				}
				break;
			case FLOAT:
				_localctx = new FloatLiteralContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(486);
				match(FLOAT);
				}
				break;
			case BOOLEAN:
				_localctx = new BooleanLiteralContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(487);
				match(BOOLEAN);
				}
				break;
			case DATE:
				_localctx = new DateLiteralContext(_localctx);
				enterOuterAlt(_localctx, 5);
				{
				setState(488);
				match(DATE);
				}
				break;
			case TIME:
				_localctx = new TimeLiteralContext(_localctx);
				enterOuterAlt(_localctx, 6);
				{
				setState(489);
				match(TIME);
				}
				break;
			case DATE_TIME:
				_localctx = new DateTimeLiteralContext(_localctx);
				enterOuterAlt(_localctx, 7);
				{
				setState(490);
				match(DATE_TIME);
				}
				break;
			case ZONED_DATE_TIME:
				_localctx = new ZonedDateTimeLiteralContext(_localctx);
				enterOuterAlt(_localctx, 8);
				{
				setState(491);
				match(ZONED_DATE_TIME);
				}
				break;
			case NUMBER_RANGE:
				_localctx = new NumberRangeLiteralContext(_localctx);
				enterOuterAlt(_localctx, 9);
				{
				setState(492);
				match(NUMBER_RANGE);
				}
				break;
			case DATE_TIME_RANGE:
				_localctx = new DateTimeRangeLiteralContext(_localctx);
				enterOuterAlt(_localctx, 10);
				{
				setState(493);
				match(DATE_TIME_RANGE);
				}
				break;
			case ENUM:
				_localctx = new EnumLiteralContext(_localctx);
				enterOuterAlt(_localctx, 11);
				{
				setState(494);
				match(ENUM);
				}
				break;
			case LOCALE:
				_localctx = new LocaleLiteralContext(_localctx);
				enterOuterAlt(_localctx, 12);
				{
				setState(495);
				match(LOCALE);
				}
				break;
			case MULTIPLE_OPENING:
				_localctx = new MultipleLiteralContext(_localctx);
				enterOuterAlt(_localctx, 13);
				{
				setState(496);
				match(MULTIPLE_OPENING);
				setState(497);
				((MultipleLiteralContext)_localctx).literal = literal();
				((MultipleLiteralContext)_localctx).values.add(((MultipleLiteralContext)_localctx).literal);
				setState(500); 
				_errHandler.sync(this);
				_la = _input.LA(1);
				do {
					{
					{
					setState(498);
					match(ARGS_DELIMITER);
					setState(499);
					((MultipleLiteralContext)_localctx).literal = literal();
					((MultipleLiteralContext)_localctx).values.add(((MultipleLiteralContext)_localctx).literal);
					}
					}
					setState(502); 
					_errHandler.sync(this);
					_la = _input.LA(1);
				} while ( _la==ARGS_DELIMITER );
				setState(504);
				match(MULTIPLE_CLOSING);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3R\u01ff\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13\t"+
		"\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
		"\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
		"\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\4\37\t\37\4 \t \4!"+
		"\t!\4\"\t\"\4#\t#\4$\t$\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\5\2R\n\2\3"+
		"\3\3\3\3\3\3\4\3\4\3\4\3\4\5\4[\n\4\3\5\3\5\3\5\3\6\3\6\3\6\3\6\3\6\3"+
		"\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6"+
		"\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3"+
		"\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6"+
		"\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\5\6\u00a2\n\6\3\7\3\7\3\7\3\7\3\7\3\7"+
		"\3\7\3\7\3\7\3\7\3\7\3\7\3\7\3\7\5\7\u00b2\n\7\3\b\3\b\3\b\3\b\3\b\3\b"+
		"\3\b\3\b\3\b\3\b\3\b\3\b\3\b\5\b\u00c1\n\b\3\b\3\b\3\b\5\b\u00c6\n\b\3"+
		"\b\3\b\3\b\5\b\u00cb\n\b\3\b\3\b\3\b\3\b\3\b\5\b\u00d2\n\b\3\b\3\b\3\b"+
		"\5\b\u00d7\n\b\3\b\3\b\3\b\3\b\3\b\5\b\u00de\n\b\3\b\3\b\3\b\3\b\3\b\3"+
		"\b\3\b\3\b\3\b\3\b\3\b\3\b\5\b\u00ec\n\b\3\t\3\t\3\t\3\t\7\t\u00f2\n\t"+
		"\f\t\16\t\u00f5\13\t\3\t\3\t\3\n\3\n\3\n\3\13\3\13\3\13\3\13\7\13\u0100"+
		"\n\13\f\13\16\13\u0103\13\13\3\13\3\13\3\f\3\f\3\f\3\f\7\f\u010b\n\f\f"+
		"\f\16\f\u010e\13\f\3\f\3\f\3\r\3\r\3\r\3\r\7\r\u0116\n\r\f\r\16\r\u0119"+
		"\13\r\3\r\3\r\3\16\3\16\3\16\3\16\3\17\3\17\3\17\3\17\3\17\3\17\3\20\3"+
		"\20\3\20\3\20\6\20\u012b\n\20\r\20\16\20\u012c\3\20\3\20\3\21\3\21\3\21"+
		"\3\21\3\21\3\21\3\21\3\21\3\22\3\22\3\22\3\22\3\23\3\23\3\23\3\23\7\23"+
		"\u0141\n\23\f\23\16\23\u0144\13\23\3\23\3\23\3\24\3\24\3\24\3\24\3\24"+
		"\3\24\3\25\3\25\3\25\3\25\7\25\u0152\n\25\f\25\16\25\u0155\13\25\3\25"+
		"\3\25\3\26\3\26\3\26\3\26\6\26\u015d\n\26\r\26\16\26\u015e\3\26\3\26\3"+
		"\27\3\27\3\27\3\27\7\27\u0167\n\27\f\27\16\27\u016a\13\27\3\27\3\27\3"+
		"\30\3\30\3\30\3\30\3\31\3\31\3\31\3\31\7\31\u0176\n\31\f\31\16\31\u0179"+
		"\13\31\3\31\3\31\3\32\3\32\3\32\3\32\6\32\u0181\n\32\r\32\16\32\u0182"+
		"\3\32\3\32\3\33\3\33\3\33\3\33\3\33\3\33\3\34\3\34\3\34\3\34\6\34\u0191"+
		"\n\34\r\34\16\34\u0192\3\34\3\34\3\35\3\35\3\35\3\35\7\35\u019b\n\35\f"+
		"\35\16\35\u019e\13\35\3\35\3\35\3\36\3\36\3\36\3\36\5\36\u01a6\n\36\3"+
		"\36\3\36\3\36\7\36\u01ab\n\36\f\36\16\36\u01ae\13\36\3\36\3\36\3\37\3"+
		"\37\3\37\3\37\3\37\5\37\u01b7\n\37\3\37\3\37\3\37\7\37\u01bc\n\37\f\37"+
		"\16\37\u01bf\13\37\5\37\u01c1\n\37\5\37\u01c3\n\37\3\37\3\37\3 \3 \3 "+
		"\3 \3 \3 \3!\3!\3!\3!\3!\3!\3\"\3\"\3\"\3\"\7\"\u01d7\n\"\f\"\16\"\u01da"+
		"\13\"\3\"\3\"\7\"\u01de\n\"\f\"\16\"\u01e1\13\"\3\"\3\"\3#\3#\3$\3$\3"+
		"$\3$\3$\3$\3$\3$\3$\3$\3$\3$\3$\3$\3$\3$\6$\u01f7\n$\r$\16$\u01f8\3$\3"+
		"$\5$\u01fd\n$\3$\2\2%\2\4\6\b\n\f\16\20\22\24\26\30\32\34\36 \"$&(*,."+
		"\60\62\64\668:<>@BDF\2\2\2\u0240\2Q\3\2\2\2\4S\3\2\2\2\6Z\3\2\2\2\b\\"+
		"\3\2\2\2\n\u00a1\3\2\2\2\f\u00b1\3\2\2\2\16\u00eb\3\2\2\2\20\u00ed\3\2"+
		"\2\2\22\u00f8\3\2\2\2\24\u00fb\3\2\2\2\26\u0106\3\2\2\2\30\u0111\3\2\2"+
		"\2\32\u011c\3\2\2\2\34\u0120\3\2\2\2\36\u0126\3\2\2\2 \u0130\3\2\2\2\""+
		"\u0138\3\2\2\2$\u013c\3\2\2\2&\u0147\3\2\2\2(\u014d\3\2\2\2*\u0158\3\2"+
		"\2\2,\u0162\3\2\2\2.\u016d\3\2\2\2\60\u0171\3\2\2\2\62\u017c\3\2\2\2\64"+
		"\u0186\3\2\2\2\66\u018c\3\2\2\28\u0196\3\2\2\2:\u01a1\3\2\2\2<\u01b1\3"+
		"\2\2\2>\u01c6\3\2\2\2@\u01cc\3\2\2\2B\u01d2\3\2\2\2D\u01e4\3\2\2\2F\u01fc"+
		"\3\2\2\2HI\5\4\3\2IJ\7\2\2\3JR\3\2\2\2KL\5\6\4\2LM\7\2\2\3MR\3\2\2\2N"+
		"O\5F$\2OP\7\2\2\3PR\3\2\2\2QH\3\2\2\2QK\3\2\2\2QN\3\2\2\2R\3\3\2\2\2S"+
		"T\7\3\2\2TU\5\20\t\2U\5\3\2\2\2V[\5\b\5\2W[\5\n\6\2X[\5\f\7\2Y[\5\16\b"+
		"\2ZV\3\2\2\2ZW\3\2\2\2ZX\3\2\2\2ZY\3\2\2\2[\7\3\2\2\2\\]\7\4\2\2]^\5."+
		"\30\2^\t\3\2\2\2_`\7\5\2\2`\u00a2\5\24\13\2ab\7\6\2\2b\u00a2\5\24\13\2"+
		"cd\7\7\2\2d\u00a2\5\24\13\2ef\7\b\2\2f\u00a2\5\24\13\2gh\7\t\2\2h\u00a2"+
		"\5\24\13\2ij\7\n\2\2j\u00a2\5\34\17\2kl\7\13\2\2l\u00a2\5\34\17\2mn\7"+
		"\f\2\2n\u00a2\5\34\17\2op\7\r\2\2p\u00a2\5\34\17\2qr\7\16\2\2r\u00a2\5"+
		"\34\17\2st\7\17\2\2t\u00a2\5 \21\2uv\7\20\2\2v\u00a2\5\36\20\2wx\7\21"+
		"\2\2x\u00a2\5\34\17\2yz\7\22\2\2z\u00a2\5\34\17\2{|\7\23\2\2|\u00a2\5"+
		"\34\17\2}~\7\24\2\2~\u00a2\5\32\16\2\177\u0080\7\25\2\2\u0080\u00a2\5"+
		"\32\16\2\u0081\u0082\7\26\2\2\u0082\u00a2\5\32\16\2\u0083\u0084\7\27\2"+
		"\2\u0084\u00a2\5\32\16\2\u0085\u0086\7\30\2\2\u0086\u00a2\5\34\17\2\u0087"+
		"\u0088\7\31\2\2\u0088\u00a2\5$\23\2\u0089\u008a\7\32\2\2\u008a\u00a2\5"+
		"\"\22\2\u008b\u008c\7\33\2\2\u008c\u00a2\5\"\22\2\u008d\u008e\7\34\2\2"+
		"\u008e\u00a2\5$\23\2\u008f\u0090\7\35\2\2\u0090\u00a2\5\"\22\2\u0091\u0092"+
		"\7\36\2\2\u0092\u00a2\5&\24\2\u0093\u0094\7\37\2\2\u0094\u00a2\5\62\32"+
		"\2\u0095\u0096\7 \2\2\u0096\u00a2\5\64\33\2\u0097\u0098\7!\2\2\u0098\u00a2"+
		"\5:\36\2\u0099\u009a\7\"\2\2\u009a\u00a2\5<\37\2\u009b\u009c\7#\2\2\u009c"+
		"\u00a2\5\22\n\2\u009d\u009e\7$\2\2\u009e\u00a2\5\22\n\2\u009f\u00a0\7"+
		"%\2\2\u00a0\u00a2\5$\23\2\u00a1_\3\2\2\2\u00a1a\3\2\2\2\u00a1c\3\2\2\2"+
		"\u00a1e\3\2\2\2\u00a1g\3\2\2\2\u00a1i\3\2\2\2\u00a1k\3\2\2\2\u00a1m\3"+
		"\2\2\2\u00a1o\3\2\2\2\u00a1q\3\2\2\2\u00a1s\3\2\2\2\u00a1u\3\2\2\2\u00a1"+
		"w\3\2\2\2\u00a1y\3\2\2\2\u00a1{\3\2\2\2\u00a1}\3\2\2\2\u00a1\177\3\2\2"+
		"\2\u00a1\u0081\3\2\2\2\u00a1\u0083\3\2\2\2\u00a1\u0085\3\2\2\2\u00a1\u0087"+
		"\3\2\2\2\u00a1\u0089\3\2\2\2\u00a1\u008b\3\2\2\2\u00a1\u008d\3\2\2\2\u00a1"+
		"\u008f\3\2\2\2\u00a1\u0091\3\2\2\2\u00a1\u0093\3\2\2\2\u00a1\u0095\3\2"+
		"\2\2\u00a1\u0097\3\2\2\2\u00a1\u0099\3\2\2\2\u00a1\u009b\3\2\2\2\u00a1"+
		"\u009d\3\2\2\2\u00a1\u009f\3\2\2\2\u00a2\13\3\2\2\2\u00a3\u00a4\7&\2\2"+
		"\u00a4\u00b2\5\26\f\2\u00a5\u00a6\7\'\2\2\u00a6\u00b2\5\32\16\2\u00a7"+
		"\u00a8\7(\2\2\u00a8\u00b2\5\32\16\2\u00a9\u00aa\7)\2\2\u00aa\u00b2\5\22"+
		"\n\2\u00ab\u00ac\7*\2\2\u00ac\u00b2\5\22\n\2\u00ad\u00ae\7+\2\2\u00ae"+
		"\u00b2\5\22\n\2\u00af\u00b0\7,\2\2\u00b0\u00b2\5\66\34\2\u00b1\u00a3\3"+
		"\2\2\2\u00b1\u00a5\3\2\2\2\u00b1\u00a7\3\2\2\2\u00b1\u00a9\3\2\2\2\u00b1"+
		"\u00ab\3\2\2\2\u00b1\u00ad\3\2\2\2\u00b1\u00af\3\2\2\2\u00b2\r\3\2\2\2"+
		"\u00b3\u00b4\7-\2\2\u00b4\u00ec\5\30\r\2\u00b5\u00b6\7.\2\2\u00b6\u00ec"+
		"\5> \2\u00b7\u00b8\7/\2\2\u00b8\u00ec\5@!\2\u00b9\u00ba\7\60\2\2\u00ba"+
		"\u00ec\5\22\n\2\u00bb\u00bc\7\61\2\2\u00bc\u00ec\5\22\n\2\u00bd\u00c0"+
		"\7\62\2\2\u00be\u00c1\5\22\n\2\u00bf\u00c1\5\"\22\2\u00c0\u00be\3\2\2"+
		"\2\u00c0\u00bf\3\2\2\2\u00c1\u00ec\3\2\2\2\u00c2\u00c5\7\63\2\2\u00c3"+
		"\u00c6\5\22\n\2\u00c4\u00c6\5(\25\2\u00c5\u00c3\3\2\2\2\u00c5\u00c4\3"+
		"\2\2\2\u00c6\u00ec\3\2\2\2\u00c7\u00ca\7\64\2\2\u00c8\u00cb\5\22\n\2\u00c9"+
		"\u00cb\5,\27\2\u00ca\u00c8\3\2\2\2\u00ca\u00c9\3\2\2\2\u00cb\u00ec\3\2"+
		"\2\2\u00cc\u00cd\7\65\2\2\u00cd\u00ec\5\"\22\2\u00ce\u00d1\7\66\2\2\u00cf"+
		"\u00d2\5\22\n\2\u00d0\u00d2\5$\23\2\u00d1\u00cf\3\2\2\2\u00d1\u00d0\3"+
		"\2\2\2\u00d2\u00ec\3\2\2\2\u00d3\u00d6\7\67\2\2\u00d4\u00d7\5\22\n\2\u00d5"+
		"\u00d7\5\30\r\2\u00d6\u00d4\3\2\2\2\u00d6\u00d5\3\2\2\2\u00d7\u00ec\3"+
		"\2\2\2\u00d8\u00d9\78\2\2\u00d9\u00ec\5B\"\2\u00da\u00dd\79\2\2\u00db"+
		"\u00de\5\22\n\2\u00dc\u00de\5\"\22\2\u00dd\u00db\3\2\2\2\u00dd\u00dc\3"+
		"\2\2\2\u00de\u00ec\3\2\2\2\u00df\u00e0\7:\2\2\u00e0\u00ec\5\62\32\2\u00e1"+
		"\u00e2\7;\2\2\u00e2\u00ec\5\62\32\2\u00e3\u00e4\7<\2\2\u00e4\u00ec\5\62"+
		"\32\2\u00e5\u00e6\7=\2\2\u00e6\u00ec\5*\26\2\u00e7\u00e8\7>\2\2\u00e8"+
		"\u00ec\5\"\22\2\u00e9\u00ea\7?\2\2\u00ea\u00ec\58\35\2\u00eb\u00b3\3\2"+
		"\2\2\u00eb\u00b5\3\2\2\2\u00eb\u00b7\3\2\2\2\u00eb\u00b9\3\2\2\2\u00eb"+
		"\u00bb\3\2\2\2\u00eb\u00bd\3\2\2\2\u00eb\u00c2\3\2\2\2\u00eb\u00c7\3\2"+
		"\2\2\u00eb\u00cc\3\2\2\2\u00eb\u00ce\3\2\2\2\u00eb\u00d3\3\2\2\2\u00eb"+
		"\u00d8\3\2\2\2\u00eb\u00da\3\2\2\2\u00eb\u00df\3\2\2\2\u00eb\u00e1\3\2"+
		"\2\2\u00eb\u00e3\3\2\2\2\u00eb\u00e5\3\2\2\2\u00eb\u00e7\3\2\2\2\u00eb"+
		"\u00e9\3\2\2\2\u00ec\17\3\2\2\2\u00ed\u00ee\7L\2\2\u00ee\u00f3\5\6\4\2"+
		"\u00ef\u00f0\7N\2\2\u00f0\u00f2\5\6\4\2\u00f1\u00ef\3\2\2\2\u00f2\u00f5"+
		"\3\2\2\2\u00f3\u00f1\3\2\2\2\u00f3\u00f4\3\2\2\2\u00f4\u00f6\3\2\2\2\u00f5"+
		"\u00f3\3\2\2\2\u00f6\u00f7\7M\2\2\u00f7\21\3\2\2\2\u00f8\u00f9\7L\2\2"+
		"\u00f9\u00fa\7M\2\2\u00fa\23\3\2\2\2\u00fb\u00fc\7L\2\2\u00fc\u0101\5"+
		"\n\6\2\u00fd\u00fe\7N\2\2\u00fe\u0100\5\n\6\2\u00ff\u00fd\3\2\2\2\u0100"+
		"\u0103\3\2\2\2\u0101\u00ff\3\2\2\2\u0101\u0102\3\2\2\2\u0102\u0104\3\2"+
		"\2\2\u0103\u0101\3\2\2\2\u0104\u0105\7M\2\2\u0105\25\3\2\2\2\u0106\u0107"+
		"\7L\2\2\u0107\u010c\5\f\7\2\u0108\u0109\7N\2\2\u0109\u010b\5\f\7\2\u010a"+
		"\u0108\3\2\2\2\u010b\u010e\3\2\2\2\u010c\u010a\3\2\2\2\u010c\u010d\3\2"+
		"\2\2\u010d\u010f\3\2\2\2\u010e\u010c\3\2\2\2\u010f\u0110\7M\2\2\u0110"+
		"\27\3\2\2\2\u0111\u0112\7L\2\2\u0112\u0117\5\16\b\2\u0113\u0114\7N\2\2"+
		"\u0114\u0116\5\16\b\2\u0115\u0113\3\2\2\2\u0116\u0119\3\2\2\2\u0117\u0115"+
		"\3\2\2\2\u0117\u0118\3\2\2\2\u0118\u011a\3\2\2\2\u0119\u0117\3\2\2\2\u011a"+
		"\u011b\7M\2\2\u011b\31\3\2\2\2\u011c\u011d\7L\2\2\u011d\u011e\5D#\2\u011e"+
		"\u011f\7M\2\2\u011f\33\3\2\2\2\u0120\u0121\7L\2\2\u0121\u0122\5D#\2\u0122"+
		"\u0123\7N\2\2\u0123\u0124\5F$\2\u0124\u0125\7M\2\2\u0125\35\3\2\2\2\u0126"+
		"\u0127\7L\2\2\u0127\u012a\5D#\2\u0128\u0129\7N\2\2\u0129\u012b\5F$\2\u012a"+
		"\u0128\3\2\2\2\u012b\u012c\3\2\2\2\u012c\u012a\3\2\2\2\u012c\u012d\3\2"+
		"\2\2\u012d\u012e\3\2\2\2\u012e\u012f\7M\2\2\u012f\37\3\2\2\2\u0130\u0131"+
		"\7L\2\2\u0131\u0132\5D#\2\u0132\u0133\7N\2\2\u0133\u0134\5F$\2\u0134\u0135"+
		"\7N\2\2\u0135\u0136\5F$\2\u0136\u0137\7M\2\2\u0137!\3\2\2\2\u0138\u0139"+
		"\7L\2\2\u0139\u013a\5F$\2\u013a\u013b\7M\2\2\u013b#\3\2\2\2\u013c\u013d"+
		"\7L\2\2\u013d\u0142\5F$\2\u013e\u013f\7N\2\2\u013f\u0141\5F$\2\u0140\u013e"+
		"\3\2\2\2\u0141\u0144\3\2\2\2\u0142\u0140\3\2\2\2\u0142\u0143\3\2\2\2\u0143"+
		"\u0145\3\2\2\2\u0144\u0142\3\2\2\2\u0145\u0146\7M\2\2\u0146%\3\2\2\2\u0147"+
		"\u0148\7L\2\2\u0148\u0149\5F$\2\u0149\u014a\7N\2\2\u014a\u014b\5F$\2\u014b"+
		"\u014c\7M\2\2\u014c\'\3\2\2\2\u014d\u014e\7L\2\2\u014e\u0153\5D#\2\u014f"+
		"\u0150\7N\2\2\u0150\u0152\5D#\2\u0151\u014f\3\2\2\2\u0152\u0155\3\2\2"+
		"\2\u0153\u0151\3\2\2\2\u0153\u0154\3\2\2\2\u0154\u0156\3\2\2\2\u0155\u0153"+
		"\3\2\2\2\u0156\u0157\7M\2\2\u0157)\3\2\2\2\u0158\u0159\7L\2\2\u0159\u015c"+
		"\5F$\2\u015a\u015b\7N\2\2\u015b\u015d\5D#\2\u015c\u015a\3\2\2\2\u015d"+
		"\u015e\3\2\2\2\u015e\u015c\3\2\2\2\u015e\u015f\3\2\2\2\u015f\u0160\3\2"+
		"\2\2\u0160\u0161\7M\2\2\u0161+\3\2\2\2\u0162\u0163\7L\2\2\u0163\u0168"+
		"\5F$\2\u0164\u0165\7N\2\2\u0165\u0167\5F$\2\u0166\u0164\3\2\2\2\u0167"+
		"\u016a\3\2\2\2\u0168\u0166\3\2\2\2\u0168\u0169\3\2\2\2\u0169\u016b\3\2"+
		"\2\2\u016a\u0168\3\2\2\2\u016b\u016c\7M\2\2\u016c-\3\2\2\2\u016d\u016e"+
		"\7L\2\2\u016e\u016f\5F$\2\u016f\u0170\7M\2\2\u0170/\3\2\2\2\u0171\u0172"+
		"\7L\2\2\u0172\u0177\5F$\2\u0173\u0174\7N\2\2\u0174\u0176\5F$\2\u0175\u0173"+
		"\3\2\2\2\u0176\u0179\3\2\2\2\u0177\u0175\3\2\2\2\u0177\u0178\3\2\2\2\u0178"+
		"\u017a\3\2\2\2\u0179\u0177\3\2\2\2\u017a\u017b\7M\2\2\u017b\61\3\2\2\2"+
		"\u017c\u017d\7L\2\2\u017d\u0180\5F$\2\u017e\u017f\7N\2\2\u017f\u0181\5"+
		"F$\2\u0180\u017e\3\2\2\2\u0181\u0182\3\2\2\2\u0182\u0180\3\2\2\2\u0182"+
		"\u0183\3\2\2\2\u0183\u0184\3\2\2\2\u0184\u0185\7M\2\2\u0185\63\3\2\2\2"+
		"\u0186\u0187\7L\2\2\u0187\u0188\5F$\2\u0188\u0189\7N\2\2\u0189\u018a\5"+
		"\n\6\2\u018a\u018b\7M\2\2\u018b\65\3\2\2\2\u018c\u018d\7L\2\2\u018d\u0190"+
		"\5F$\2\u018e\u018f\7N\2\2\u018f\u0191\5\f\7\2\u0190\u018e\3\2\2\2\u0191"+
		"\u0192\3\2\2\2\u0192\u0190\3\2\2\2\u0192\u0193\3\2\2\2\u0193\u0194\3\2"+
		"\2\2\u0194\u0195\7M\2\2\u0195\67\3\2\2\2\u0196\u0197\7L\2\2\u0197\u019c"+
		"\5F$\2\u0198\u0199\7N\2\2\u0199\u019b\5\16\b\2\u019a\u0198\3\2\2\2\u019b"+
		"\u019e\3\2\2\2\u019c\u019a\3\2\2\2\u019c\u019d\3\2\2\2\u019d\u019f\3\2"+
		"\2\2\u019e\u019c\3\2\2\2\u019f\u01a0\7M\2\2\u01a09\3\2\2\2\u01a1\u01a5"+
		"\7L\2\2\u01a2\u01a3\5F$\2\u01a3\u01a4\7N\2\2\u01a4\u01a6\3\2\2\2\u01a5"+
		"\u01a2\3\2\2\2\u01a5\u01a6\3\2\2\2\u01a6\u01a7\3\2\2\2\u01a7\u01ac\5F"+
		"$\2\u01a8\u01a9\7N\2\2\u01a9\u01ab\5\n\6\2\u01aa\u01a8\3\2\2\2\u01ab\u01ae"+
		"\3\2\2\2\u01ac\u01aa\3\2\2\2\u01ac\u01ad\3\2\2\2\u01ad\u01af\3\2\2\2\u01ae"+
		"\u01ac\3\2\2\2\u01af\u01b0\7M\2\2\u01b0;\3\2\2\2\u01b1\u01c2\7L\2\2\u01b2"+
		"\u01c3\5F$\2\u01b3\u01b4\5F$\2\u01b4\u01b5\7N\2\2\u01b5\u01b7\3\2\2\2"+
		"\u01b6\u01b3\3\2\2\2\u01b6\u01b7\3\2\2\2\u01b7\u01c0\3\2\2\2\u01b8\u01bd"+
		"\5\n\6\2\u01b9\u01ba\7N\2\2\u01ba\u01bc\5\n\6\2\u01bb\u01b9\3\2\2\2\u01bc"+
		"\u01bf\3\2\2\2\u01bd\u01bb\3\2\2\2\u01bd\u01be\3\2\2\2\u01be\u01c1\3\2"+
		"\2\2\u01bf\u01bd\3\2\2\2\u01c0\u01b8\3\2\2\2\u01c0\u01c1\3\2\2\2\u01c1"+
		"\u01c3\3\2\2\2\u01c2\u01b2\3\2\2\2\u01c2\u01b6\3\2\2\2\u01c3\u01c4\3\2"+
		"\2\2\u01c4\u01c5\7M\2\2\u01c5=\3\2\2\2\u01c6\u01c7\7L\2\2\u01c7\u01c8"+
		"\5F$\2\u01c8\u01c9\7N\2\2\u01c9\u01ca\5F$\2\u01ca\u01cb\7M\2\2\u01cb?"+
		"\3\2\2\2\u01cc\u01cd\7L\2\2\u01cd\u01ce\5F$\2\u01ce\u01cf\7N\2\2\u01cf"+
		"\u01d0\5F$\2\u01d0\u01d1\7M\2\2\u01d1A\3\2\2\2\u01d2\u01d3\7L\2\2\u01d3"+
		"\u01d8\5F$\2\u01d4\u01d5\7N\2\2\u01d5\u01d7\5F$\2\u01d6\u01d4\3\2\2\2"+
		"\u01d7\u01da\3\2\2\2\u01d8\u01d6\3\2\2\2\u01d8\u01d9\3\2\2\2\u01d9\u01df"+
		"\3\2\2\2\u01da\u01d8\3\2\2\2\u01db\u01dc\7N\2\2\u01dc\u01de\5\16\b\2\u01dd"+
		"\u01db\3\2\2\2\u01de\u01e1\3\2\2\2\u01df\u01dd\3\2\2\2\u01df\u01e0\3\2"+
		"\2\2\u01e0\u01e2\3\2\2\2\u01e1\u01df\3\2\2\2\u01e2\u01e3\7M\2\2\u01e3"+
		"C\3\2\2\2\u01e4\u01e5\7@\2\2\u01e5E\3\2\2\2\u01e6\u01fd\7@\2\2\u01e7\u01fd"+
		"\7A\2\2\u01e8\u01fd\7B\2\2\u01e9\u01fd\7C\2\2\u01ea\u01fd\7D\2\2\u01eb"+
		"\u01fd\7E\2\2\u01ec\u01fd\7F\2\2\u01ed\u01fd\7G\2\2\u01ee\u01fd\7H\2\2"+
		"\u01ef\u01fd\7I\2\2\u01f0\u01fd\7J\2\2\u01f1\u01fd\7K\2\2\u01f2\u01f3"+
		"\7O\2\2\u01f3\u01f6\5F$\2\u01f4\u01f5\7N\2\2\u01f5\u01f7\5F$\2\u01f6\u01f4"+
		"\3\2\2\2\u01f7\u01f8\3\2\2\2\u01f8\u01f6\3\2\2\2\u01f8\u01f9\3\2\2\2\u01f9"+
		"\u01fa\3\2\2\2\u01fa\u01fb\7P\2\2\u01fb\u01fd\3\2\2\2\u01fc\u01e6\3\2"+
		"\2\2\u01fc\u01e7\3\2\2\2\u01fc\u01e8\3\2\2\2\u01fc\u01e9\3\2\2\2\u01fc"+
		"\u01ea\3\2\2\2\u01fc\u01eb\3\2\2\2\u01fc\u01ec\3\2\2\2\u01fc\u01ed\3\2"+
		"\2\2\u01fc\u01ee\3\2\2\2\u01fc\u01ef\3\2\2\2\u01fc\u01f0\3\2\2\2\u01fc"+
		"\u01f1\3\2\2\2\u01fc\u01f2\3\2\2\2\u01fdG\3\2\2\2$QZ\u00a1\u00b1\u00c0"+
		"\u00c5\u00ca\u00d1\u00d6\u00dd\u00eb\u00f3\u0101\u010c\u0117\u012c\u0142"+
		"\u0153\u015e\u0168\u0177\u0182\u0192\u019c\u01a5\u01ac\u01b6\u01bd\u01c0"+
		"\u01c2\u01d8\u01df\u01f8\u01fc";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}