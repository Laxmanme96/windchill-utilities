/*     */package ext.emerson.query;

/*     */
/*     */import java.math.BigDecimal;
/*     */
import java.util.ArrayList;
/*     */
import java.util.Collections;
/*     */
import java.util.HashSet;
/*     */
import java.util.Iterator;
/*     */
import java.util.Set;
/*     */
import java.util.Vector;

/*     */
import wt.admin.AdministrativeDomain;
/*     */
import wt.fc.PersistenceServerHelper;
/*     */
import wt.fc.QueryResult;
/*     */
import wt.introspection.ClassInfo;
/*     */
import wt.org.WTGroup;
/*     */
import wt.org.WTPrincipal;
/*     */
import wt.org.WTUser;
/*     */
import wt.pds.DatabaseInfoUtilities;
/*     */
import wt.query.AbstractClassTableExpression;
/*     */
import wt.query.ArrayExpression;
/*     */
import wt.query.BasicPageableQuerySpec;
/*     */
import wt.query.ClassAttribute;
/*     */
import wt.query.ClassTableExpression;
/*     */
import wt.query.CompositeQuerySpec;
/*     */
import wt.query.ConstantExpression;
/*     */
import wt.query.ExistsExpression;
/*     */
import wt.query.NegatedExpression;
/*     */
import wt.query.OrderBy;
/*     */
import wt.query.QueryException;
/*     */
import wt.query.QuerySpec;
/*     */
import wt.query.SQLFunction;
/*     */
import wt.query.SearchCondition;
/*     */
import wt.query.SubSelectExpression;
/*     */
import wt.query.TableExpression;
/*     */
import wt.ufid.OwningRepositoryLocalObject;
/*     */
import wt.ufid.RemoteObjectId;
/*     */
import wt.ufid.RemoteObjectInfo;
/*     */
import wt.ufid.Repository;
/*     */
import wt.util.WTException;
/*     */
import wt.util.WTPropertyVetoException;

/*     */
/*     */
/*     */
/*     */
/*     */
/*     */
/*     */public final class QueryUtil
/*     */ {
/*     */
/*     */private static QuerySpec getBaseQueryNotDisabled(Class paramClass, boolean paramBoolean) throws WTException {
	/* 316 */QuerySpec localQuerySpec = new QuerySpec();
	/* 317 */int i = localQuerySpec.appendClassList(paramClass, true);
	/* 318 */if (paramBoolean) {
		/*     */try {
			/* 320 */Set localSet = getValidSubClasses(paramClass);
			/* 321 */((AbstractClassTableExpression) localQuerySpec.getFromClause().getTableExpressionAt(i))
					.setExcludedDescendants(new ArrayList(localSet));
			/*     */}
		/*     */catch (WTPropertyVetoException localWTPropertyVetoException) {
			/* 324 */throw new WTException(localWTPropertyVetoException);
			/*     */}
		/*     */}
	/* 327 */localQuerySpec.appendWhere(new SearchCondition(paramClass, "disabled", "FALSE"), new int[] { i });
	/*     */
	/* 329 */return localQuerySpec;
	/*     */}

/*     */protected static BasicPageableQuerySpec getPageableQuerySpec(QuerySpec paramQuerySpec, int paramInt1,
		int paramInt2)/*     */ throws WTPropertyVetoException
/*     */ {
	/* 58 */BasicPageableQuerySpec localBasicPageableQuerySpec = new BasicPageableQuerySpec();
	/* 59 */localBasicPageableQuerySpec.setPrimaryStatement(paramQuerySpec);
	/* 60 */localBasicPageableQuerySpec.setOffset(paramInt2);
	/* 61 */localBasicPageableQuerySpec.setRange(paramInt1);
	/* 62 */localBasicPageableQuerySpec.setBackgroundThreadEnabled(false);
	/* 63 */return localBasicPageableQuerySpec;
	/*     */}

/*     */
/*     */protected static String getPrincipalName(Object paramObject)/*     */ throws WTException
/*     */ {
	/* 429 */QuerySpec localQuerySpec = new QuerySpec();
	/* 430 */BigDecimal localBigDecimal = (BigDecimal) paramObject;
	/* 431 */Long localLong = Long.valueOf(localBigDecimal.longValue());
	/* 432 */int i = localQuerySpec.appendClassList(WTPrincipal.class, false);
	/* 433 */localQuerySpec.appendSelectAttribute("name", i, false);
	/* 434 */SearchCondition localSearchCondition = new SearchCondition(WTPrincipal.class,
			"thePersistInfo.theObjectIdentifier.id", "=", localLong);
	/* 435 */localQuerySpec.appendWhere(localSearchCondition, new int[] { i });
	/*     */
	/* 437 */QueryResult localQueryResult = PersistenceServerHelper.manager.query(localQuerySpec);
	/*     */
	/* 439 */if (localQueryResult.hasMoreElements()) {
		/* 440 */Object[] arrayOfObject = (Object[]) localQueryResult.nextElement();
		/* 441 */return ((String) arrayOfObject[0]);
		/*     */}
	/* 443 */return null;
	/*     */}

/*     */
/*     */protected static QuerySpec getQuerySpecForLdapLinkagesCheck(Class<?> paramClass)
		throws QueryException, WTPropertyVetoException
/*     */ {
	/* 208 */QuerySpec localQuerySpec = new QuerySpec();
	/* 209 */localQuerySpec.setDescendantQuery(false);
	/* 210 */int i = localQuerySpec.appendClassList(paramClass, true);
	/* 211 */int j = localQuerySpec.appendClassList(RemoteObjectInfo.class, true);
	/* 212 */int k = localQuerySpec.appendClassList(RemoteObjectId.class, false);
	/*     */
	/* 214 */if (paramClass.isAssignableFrom(WTGroup.class)) {
		/* 215 */localQuerySpec.appendWhere(new SearchCondition(WTGroup.class, "internal", "FALSE"), new int[] { i });
		/*     */
		/* 217 */localQuerySpec.appendAnd();
		/*     */}
	/*     */
	/* 220 */localQuerySpec.appendWhere(
			new SearchCondition(new ClassAttribute(RemoteObjectInfo.class, "remoteId"), "=",
					new ClassAttribute(RemoteObjectId.class, "thePersistInfo.theObjectIdentifier.id")),
			new int[] { j, k });
	/*     */
	/* 223 */localQuerySpec.appendAnd();
	/*     */
	/* 225 */SearchCondition localSearchCondition = new SearchCondition(
			new ClassAttribute(RemoteObjectInfo.class, "localObjectReference.key.id"), "=",
			new ClassAttribute(paramClass, "thePersistInfo.theObjectIdentifier.id"));
	/*     */
	/* 232 */localSearchCondition.setOuterJoin(1);
	/* 233 */localQuerySpec.appendWhere(localSearchCondition, new int[] { j, i });
	/*     */
	/* 235 */localQuerySpec.appendAnd();
	/*     */
	/* 237 */localSearchCondition = new SearchCondition(
			new ClassAttribute(RemoteObjectInfo.class, "localObjectReference.key.classname"), "=",
			new ClassAttribute(paramClass, "thePersistInfo.theObjectIdentifier.classname"));
	/*     */
	/* 244 */localSearchCondition.setOuterJoin(1);
	/* 245 */localQuerySpec.appendWhere(localSearchCondition, new int[] { j, i });
	/*     */
	/* 247 */return localQuerySpec;
	/*     */}

/*     */
/*     */protected static QuerySpec getQuerySpecForLdapToDatabaseCheck(Repository paramRepository, String paramString)
		throws QueryException, WTPropertyVetoException
/*     */ {
	/* 252 */QuerySpec localQuerySpec = new QuerySpec();
	/* 253 */localQuerySpec.setDescendantQuery(true);
	/* 254 */int i = localQuerySpec.appendClassList(RemoteObjectInfo.class, true);
	/* 255 */int j = localQuerySpec.appendClassList(WTGroup.class, true);
	/* 256 */int k = localQuerySpec.appendClassList(RemoteObjectId.class, false);
	/*     */
	/* 258 */localQuerySpec.appendWhere(new SearchCondition(RemoteObjectInfo.class, "birthId", "=",
			paramRepository.getPersistInfo().getObjectIdentifier().getId()), new int[] { i });
	/*     */
	/* 260 */localQuerySpec.appendAnd();
	/*     */
	/* 262 */localQuerySpec.appendWhere(
			new SearchCondition(new ClassAttribute(RemoteObjectInfo.class, "remoteId"), "=",
					new ClassAttribute(RemoteObjectId.class, "thePersistInfo.theObjectIdentifier.id")),
			new int[] { i, k });
	/*     */
	/* 265 */localQuerySpec.appendAnd();
	/*     */
	/* 267 */localQuerySpec.appendWhere(new SearchCondition(RemoteObjectId.class, "remoteObjectId", "=", paramString),
			new int[] { k });
	/*     */
	/* 269 */localQuerySpec.appendAnd();
	/*     */
	/* 271 */SearchCondition localSearchCondition1 = new SearchCondition(RemoteObjectInfo.class,
			"localObjectReference.key.classname", "=", WTGroup.class.getName());
	/*     */
	/* 274 */localQuerySpec.appendWhere(localSearchCondition1, new int[] { i });
	/* 275 */localQuerySpec.appendAnd();
	/*     */
	/* 277 */SearchCondition localSearchCondition2 = new SearchCondition(
			new ClassAttribute(WTGroup.class, "thePersistInfo.theObjectIdentifier.id"), "=",
			new ClassAttribute(RemoteObjectInfo.class, "localObjectReference.key.id"));
	/*     */
	/* 283 */localSearchCondition2.setOuterJoin(1);
	/* 284 */localQuerySpec.appendWhere(localSearchCondition2, new int[] { j, i });
	/*     */
	/* 286 */localQuerySpec.setAdvancedQueryEnabled(true);
	/* 287 */return localQuerySpec;
	/*     */}

/*     */
/*     */protected static QuerySpec getQuerySpecForPrincipalReferencesCheck(String[] paramArrayOfString)
		throws QueryException, WTPropertyVetoException
/*     */ {
	/* 109 */QuerySpec localQuerySpec1 = new QuerySpec();
	/* 110 */localQuerySpec1.setAdvancedQueryEnabled(true);
	/* 111 */int i = localQuerySpec1.addClassList(OwningRepositoryLocalObject.class, true);
	/*     */
	/* 113 */SearchCondition localSearchCondition = new SearchCondition(
			new ClassAttribute(OwningRepositoryLocalObject.class, "roleBObjectRef.key.classname"), "IN",
			new ArrayExpression(paramArrayOfString));
	/*     */
	/* 116 */localQuerySpec1.appendWhere(localSearchCondition, new int[] { i });
	/*     */
	/* 118 */localQuerySpec1.appendAnd();
	/*     */
	/* 120 */QuerySpec localQuerySpec2 = new QuerySpec();
	/* 121 */localQuerySpec2.setAdvancedQueryEnabled(true);
	/* 122 */localQuerySpec2.getFromClause().setAliasPrefix("B");
	/*     */
	/* 124 */int j = localQuerySpec2.addClassList(WTPrincipal.class, false);
	/* 125 */localQuerySpec2.appendSelectAttribute("thePersistInfo.theObjectIdentifier.id", j, true);
	/*     */
	/* 127 */TableExpression[] arrayOfTableExpression = { localQuerySpec2.getFromClause().getTableExpressionAt(j),
			localQuerySpec1.getFromClause().getTableExpressionAt(i) };
	/*     */
	/* 132 */String[] arrayOfString = { localQuerySpec2.getFromClause().getAliasAt(j),
			localQuerySpec1.getFromClause().getAliasAt(i) };
	/*     */
	/* 138 */localQuerySpec2.appendWhere(
			new SearchCondition(new ClassAttribute(WTPrincipal.class, "thePersistInfo.theObjectIdentifier.id"), "=",
					new ClassAttribute(OwningRepositoryLocalObject.class, "roleBObjectRef.key.id")),
			arrayOfTableExpression, arrayOfString);
	/*     */
	/* 147 */ExistsExpression localExistsExpression = new ExistsExpression(localQuerySpec2);
	/* 148 */localExistsExpression.setAdvancedQueryEnabled(true);
	/*     */
	/* 150 */localQuerySpec1.appendWhere(new NegatedExpression(localExistsExpression), null);
	/* 151 */return localQuerySpec1;
	/*     */}

/*     */
/*     */protected static QuerySpec getQuerySpecForRepositoryReferencesCheck(String[] paramArrayOfString)
		throws QueryException, WTPropertyVetoException
/*     */ {
	/* 156 */QuerySpec localQuerySpec1 = new QuerySpec();
	/* 157 */localQuerySpec1.setAdvancedQueryEnabled(true);
	/* 158 */int i = localQuerySpec1.addClassList(OwningRepositoryLocalObject.class, true);
	/*     */
	/* 160 */SearchCondition localSearchCondition = new SearchCondition(
			new ClassAttribute(OwningRepositoryLocalObject.class, "roleBObjectRef.key.classname"), "IN",
			new ArrayExpression(paramArrayOfString));
	/*     */
	/* 163 */localQuerySpec1.appendWhere(localSearchCondition, new int[] { i });
	/*     */
	/* 165 */localQuerySpec1.appendAnd();
	/*     */
	/* 167 */QuerySpec localQuerySpec2 = new QuerySpec();
	/* 168 */localQuerySpec2.setAdvancedQueryEnabled(true);
	/* 169 */localQuerySpec2.getFromClause().setAliasPrefix("B");
	/*     */
	/* 171 */int j = localQuerySpec2.addClassList(Repository.class, false);
	/* 172 */localQuerySpec2.appendSelectAttribute("thePersistInfo.theObjectIdentifier.id", j, true);
	/*     */
	/* 174 */TableExpression[] arrayOfTableExpression = { localQuerySpec2.getFromClause().getTableExpressionAt(j),
			localQuerySpec1.getFromClause().getTableExpressionAt(i) };
	/*     */
	/* 177 */String[] arrayOfString = { localQuerySpec2.getFromClause().getAliasAt(j),
			localQuerySpec1.getFromClause().getAliasAt(i) };
	/*     */
	/* 179 */localQuerySpec2.appendWhere(
			new SearchCondition(new ClassAttribute(Repository.class, "thePersistInfo.theObjectIdentifier.id"), "=",
					new ClassAttribute(OwningRepositoryLocalObject.class, "roleAObjectRef.key.id")),
			arrayOfTableExpression, arrayOfString);
	/*     */
	/* 188 */localQuerySpec2.appendAnd();
	/*     */
	/* 190 */localQuerySpec2.appendWhere(
			new SearchCondition(new ClassAttribute(Repository.class, "thePersistInfo.theObjectIdentifier.classname"),
					"=", new ClassAttribute(OwningRepositoryLocalObject.class, "roleAObjectRef.key.classname")),
			arrayOfTableExpression, arrayOfString);
	/*     */
	/* 199 */ExistsExpression localExistsExpression = new ExistsExpression(localQuerySpec2);
	/* 200 */localExistsExpression.setAdvancedQueryEnabled(true);
	/*     */
	/* 202 */localQuerySpec1.appendWhere(new NegatedExpression(localExistsExpression), null);
	/* 203 */return localQuerySpec1;
	/*     */}

/*     */
/*     */protected static QuerySpec getQuerySpecToCheckDuplicateORLOREntries()
		/*     */ throws QueryException, WTPropertyVetoException
/*     */ {
	/* 395 */QuerySpec localQuerySpec1 = new QuerySpec();
	/* 396 */int i = localQuerySpec1.appendClassList(OwningRepositoryLocalObject.class, false);
	/* 397 */int j = localQuerySpec1.appendClassList(Repository.class, false);
	/* 398 */localQuerySpec1.appendSelectAttribute("thePersistInfo.theObjectIdentifier.id", i, false);
	/* 399 */localQuerySpec1.appendSelectAttribute("roleAObjectRef.key.id", i, false);
	/* 400 */localQuerySpec1.appendSelectAttribute("roleBObjectRef.key.id", i, false);
	/* 401 */localQuerySpec1.appendSelectAttribute("roleBObjectRef.key.classname", i, false);
	/* 402 */localQuerySpec1.appendSelectAttribute("lastKnownDomain", j, false);
	/* 403 */localQuerySpec1.setAdvancedQueryEnabled(true);
	/*     */
	/* 405 */QuerySpec localQuerySpec2 = new QuerySpec();
	/* 406 */localQuerySpec2.setAdvancedQueryEnabled(true);
	/* 407 */localQuerySpec2.getFromClause().setAliasPrefix("B");
	/* 408 */int k = localQuerySpec2.appendClassList(OwningRepositoryLocalObject.class, false);
	/* 409 */int[] arrayOfInt = { k };
	/* 410 */localQuerySpec2.appendGroupBy(
			new ClassAttribute(OwningRepositoryLocalObject.class, "roleBObjectRef.key.id"), arrayOfInt, true);
	/* 411 */SQLFunction localSQLFunction = SQLFunction.newSQLFunction("COUNT",
			new ClassAttribute(OwningRepositoryLocalObject.class, "roleBObjectRef.key.id"));
	/* 412 */localQuerySpec2.appendHaving(
			new SearchCondition(localSQLFunction, ">", new ConstantExpression(Integer.valueOf(1))), arrayOfInt);
	/*     */
	/* 414 */SearchCondition localSearchCondition1 = new SearchCondition(
			new ClassAttribute(OwningRepositoryLocalObject.class, "roleBObjectRef.key.id"), "IN",
			new SubSelectExpression(localQuerySpec2));
	/* 415 */localQuerySpec1.appendWhere(localSearchCondition1, new int[] { i });
	/* 416 */localQuerySpec1.appendAnd();
	/* 417 */SearchCondition localSearchCondition2 = new SearchCondition(
			new ClassAttribute(Repository.class, "thePersistInfo.theObjectIdentifier.id"), "=",
			new ClassAttribute(OwningRepositoryLocalObject.class, "roleAObjectRef.key.id"));
	/* 418 */localQuerySpec1.appendWhere(localSearchCondition2, new int[] { j, i });
	/* 419 */localQuerySpec1.appendOrderBy(
			new OrderBy(new ClassAttribute(OwningRepositoryLocalObject.class, "roleBObjectRef.key.id"), false),
			new int[] { i });
	/* 420 */return localQuerySpec1;
	/*     */}

/*     */
/*     */protected static QuerySpec getQuerySpecToCheckDuplicateRemoteObjectInfoEntries()
		/*     */ throws QueryException, WTPropertyVetoException
/*     */ {
	/* 354 */QuerySpec localQuerySpec1 = new QuerySpec();
	/* 355 */localQuerySpec1.setAdvancedQueryEnabled(true);
	/* 356 */int i = localQuerySpec1.appendClassList(RemoteObjectInfo.class, false);
	/* 357 */int j = localQuerySpec1.appendClassList(RemoteObjectId.class, false);
	/* 358 */int k = localQuerySpec1.appendClassList(Repository.class, false);
	/* 359 */localQuerySpec1.appendSelectAttribute("thePersistInfo.theObjectIdentifier.id", i, false);
	/* 360 */localQuerySpec1.appendSelectAttribute("localObjectReference.key.id", i, false);
	/* 361 */localQuerySpec1.appendSelectAttribute("remoteObjectId", j, false);
	/* 362 */localQuerySpec1.appendSelectAttribute("lastKnownDomain", k, false);
	/*     */
	/* 365 */QuerySpec localQuerySpec2 = new QuerySpec();
	/* 366 */localQuerySpec2.setAdvancedQueryEnabled(true);
	/* 367 */localQuerySpec2.getFromClause().setAliasPrefix("B");
	/* 368 */int l = localQuerySpec2.appendClassList(RemoteObjectInfo.class, false);
	/* 369 */localQuerySpec2.appendGroupBy(new ClassAttribute(RemoteObjectInfo.class, "localObjectReference.key.id"),
			new int[] { l }, true);
	/* 370 */SQLFunction localSQLFunction = SQLFunction.newSQLFunction("COUNT",
			new ClassAttribute(RemoteObjectInfo.class, "localObjectReference.key.id"));
	/* 371 */localQuerySpec2.appendHaving(
			new SearchCondition(localSQLFunction, ">", new ConstantExpression(Integer.valueOf(1))), new int[] { l });
	/*     */
	/* 373 */SearchCondition localSearchCondition1 = new SearchCondition(
			new ClassAttribute(RemoteObjectInfo.class, "localObjectReference.key.id"), "IN",
			new SubSelectExpression(localQuerySpec2));
	/* 374 */localQuerySpec1.appendWhere(localSearchCondition1, new int[] { i });
	/* 375 */localQuerySpec1.appendAnd();
	/* 376 */SearchCondition localSearchCondition2 = new SearchCondition(
			new ClassAttribute(RemoteObjectInfo.class, "remoteId"), "=",
			new ClassAttribute(RemoteObjectId.class, "thePersistInfo.theObjectIdentifier.id"));
	/* 377 */localQuerySpec1.appendWhere(localSearchCondition2, new int[] { i, j });
	/* 378 */localQuerySpec1.appendAnd();
	/* 379 */SearchCondition localSearchCondition3 = new SearchCondition(
			new ClassAttribute(RemoteObjectInfo.class, "lastKnownId"), "=",
			new ClassAttribute(Repository.class, "thePersistInfo.theObjectIdentifier.id"));
	/* 380 */localQuerySpec1.appendWhere(localSearchCondition3, new int[] { i, k });
	/* 381 */localQuerySpec1.appendOrderBy(
			new OrderBy(new ClassAttribute(RemoteObjectInfo.class, "localObjectReference.key.id"), false),
			new int[] { i });
	/* 382 */return localQuerySpec1;
	/*     */}

/*     */
/*     */protected static QuerySpec getQuerySpecToCheckPrincipalsWithoutDomains(Class paramClass)
		/*     */ throws QueryException, WTPropertyVetoException
/*     */ {
	/* 79 */QuerySpec localQuerySpec1 = new QuerySpec();
	/* 80 */localQuerySpec1.appendSelect(new ConstantExpression(Integer.valueOf(1)), false);
	/* 81 */int i = localQuerySpec1.appendFrom(new ClassTableExpression(AdministrativeDomain.class));
	/* 82 */localQuerySpec1.getFromClause().setAliasPrefix("B");
	/*     */
	/* 85 */SearchCondition localSearchCondition = new SearchCondition(
			new ClassAttribute(AdministrativeDomain.class, "thePersistInfo.theObjectIdentifier.id"), "=",
			new ClassAttribute(paramClass, "domainRef.key.id"));
	/*     */
	/* 89 */QuerySpec localQuerySpec2 = new QuerySpec();
	/* 90 */int j = localQuerySpec2.appendClassList(paramClass, true);
	/*     */
	/* 92 */TableExpression[] arrayOfTableExpression = { localQuerySpec1.getFromClause().getTableExpressionAt(i),
			localQuerySpec2.getFromClause().getTableExpressionAt(j) };
	/*     */
	/* 96 */String[] arrayOfString = { localQuerySpec1.getFromClause().getAliasAt(i),
			localQuerySpec2.getFromClause().getAliasAt(j) };
	/*     */
	/* 99 */localQuerySpec1.appendWhere(localSearchCondition, arrayOfTableExpression, arrayOfString);
	/*     */
	/* 101 */NegatedExpression localNegatedExpression = new NegatedExpression(new ExistsExpression(localQuerySpec1));
	/* 102 */localQuerySpec2.appendWhere(localNegatedExpression, new int[] { 0, 1 });
	/* 103 */localQuerySpec2.setAdvancedQueryEnabled(true);
	/* 104 */return localQuerySpec2;
	/*     */}

/*     */
/*     */protected static QuerySpec getQuerySpecToCheckUFID(Class paramClass)/*     */ throws QueryException
/*     */ {
	/* 522 */QuerySpec localQuerySpec = new QuerySpec();
	/* 523 */int i = localQuerySpec.appendClassList(paramClass, true);
	/* 524 */int j = localQuerySpec.appendClassList(RemoteObjectInfo.class, true);
	/* 525 */SearchCondition localSearchCondition1 = new SearchCondition(paramClass, "disabled", "FALSE");
	/* 526 */SearchCondition localSearchCondition2 = new SearchCondition(paramClass, "internal", "TRUE");
	/* 527 */SearchCondition localSearchCondition3 = new SearchCondition(
			new ClassAttribute(paramClass, "thePersistInfo.theObjectIdentifier.id"), "=",
			new ClassAttribute(RemoteObjectInfo.class, "localObjectReference.key.id"));
	/* 528 */localQuerySpec.appendWhere(localSearchCondition1, new int[] { i });
	/* 529 */localQuerySpec.appendAnd();
	/* 530 */localQuerySpec.appendWhere(localSearchCondition2, new int[] { i });
	/* 531 */localQuerySpec.appendAnd();
	/* 532 */localQuerySpec.appendWhere(localSearchCondition3, new int[] { i, j });
	/* 533 */return localQuerySpec;
	/*     */}
/*     */

/*     */
/*     */public static CompositeQuerySpec getQuerySpeForPrincipalsInRepairNeeded() throws WTException {
	/* 291 */CompositeQuerySpec localCompositeQuerySpec = new CompositeQuerySpec();
	/*     */
	/* 293 */QuerySpec localQuerySpec1 = getBaseQueryNotDisabled(WTUser.class, true);
	/*     */
	/* 295 */localQuerySpec1.appendAnd();
	/* 296 */localQuerySpec1.appendWhere(new SearchCondition(WTUser.class, "internal", "FALSE"),
			localQuerySpec1.getResultIndicies());
	/*     */
	/* 298 */localQuerySpec1.appendAnd();
	/* 299 */localQuerySpec1.appendWhere(new SearchCondition(WTUser.class, "repairNeeded", "TRUE"),
			localQuerySpec1.getResultIndicies());
	/*     */
	/* 301 */localCompositeQuerySpec.addComponent(localQuerySpec1);
	/*     */
	/* 304 */QuerySpec localQuerySpec2 = getBaseQueryNotDisabled(WTGroup.class, false);
	/* 305 */localQuerySpec2.appendAnd();
	/* 306 */localQuerySpec2.appendWhere(new SearchCondition(WTGroup.class, "internal", "FALSE"),
			localQuerySpec2.getResultIndicies());
	/*     */
	/* 308 */localQuerySpec2.appendAnd();
	/* 309 */localQuerySpec2.appendWhere(new SearchCondition(WTGroup.class, "repairNeeded", "TRUE"),
			localQuerySpec2.getResultIndicies());
	/*     */
	/* 311 */localCompositeQuerySpec.addComponent(localQuerySpec2);
	/* 312 */return localCompositeQuerySpec;
	/*     */}

/*     */
/*     */protected static QuerySpec getQueryToRetriveGroups(Class paramClass, boolean paramBoolean)
		/*     */ throws WTException, WTPropertyVetoException
/*     */ {
	/* 491 */QuerySpec localQuerySpec = new QuerySpec();
	/* 492 */int i = localQuerySpec.appendClassList(paramClass, true);
	/* 493 */AbstractClassTableExpression localAbstractClassTableExpression = (AbstractClassTableExpression) localQuerySpec
			.getFromClause().getTableExpressionAt(i);
	/*     */
	/* 495 */Set localSet = getValidSubClasses(paramClass);
	/* 496 */localAbstractClassTableExpression.setExcludedDescendants(new ArrayList(localSet));
	/*     */
	/* 498 */SearchCondition localSearchCondition1 = new SearchCondition(paramClass, "disabled", "FALSE");
	/*     */
	/* 500 */localQuerySpec.appendWhere(localSearchCondition1, new int[] { i });
	/* 501 */localQuerySpec.appendAnd();
	/* 502 */SearchCondition localSearchCondition2 = new SearchCondition(paramClass, "internal", "FALSE");
	/*     */
	/* 506 */localQuerySpec.appendWhere(localSearchCondition2, new int[] { i });
	/*     */
	/* 508 */return localQuerySpec;
	/*     */}

/*     */
/*     */protected static QuerySpec getQueryToRetriveOrgs(Class paramClass)/*     */ throws QueryException
/*     */ {
	/* 474 */QuerySpec localQuerySpec = new QuerySpec();
	/* 475 */int i = localQuerySpec.appendClassList(paramClass, true);
	/* 476 */SearchCondition localSearchCondition = new SearchCondition(paramClass, "disabled", "FALSE");
	/* 477 */localQuerySpec.appendWhere(localSearchCondition, new int[] { i });
	/* 478 */return localQuerySpec;
	/*     */}

/*     */
/*     */protected static QuerySpec getQueryToRetriveUsers(Class paramClass, boolean paramBoolean)
		/*     */ throws QueryException
/*     */ {
	/* 453 */QuerySpec localQuerySpec = new QuerySpec();
	/* 454 */int i = localQuerySpec.appendClassList(paramClass, true);
	/* 455 */SearchCondition localSearchCondition1 = new SearchCondition(paramClass, "disabled", "FALSE");
	/* 456 */localQuerySpec.appendWhere(localSearchCondition1, new int[] { i });
	/* 457 */localQuerySpec.appendAnd();
	/*     */SearchCondition localSearchCondition2;
	/* 459 */if (paramBoolean)
		/* 460 */localSearchCondition2 = new SearchCondition(paramClass, "internal", "TRUE");
	/*     */else
		/* 462 */localSearchCondition2 = new SearchCondition(paramClass, "internal", "FALSE");
	/* 463 */localQuerySpec.appendWhere(localSearchCondition2, new int[] { i });
	/* 464 */return localQuerySpec;
	/*     */}

/*     */
/*     */private static Set<Class<?>> getValidSubClasses(Class paramClass) throws WTException {
	/* 333 */Vector localVector = DatabaseInfoUtilities.getValidSubClassInfos(paramClass);
	/* 334 */HashSet localHashSet = new HashSet(localVector.size());
	/* 335 */for (Iterator localIterator = localVector.iterator(); localIterator.hasNext();) {
		/* 336 */Class localClass = ((ClassInfo) localIterator.next()).getBusinessClass();
		/* 337 */if (!(paramClass.equals(localClass))) {
			/* 338 */localHashSet.add(localClass);
			/*     */}
		/*     */}
	/* 341 */return Collections.unmodifiableSet(localHashSet);
	/*     */}
}

/*
 * Location: C:\ptc\Windchill_10.2\Windchill\lib\wnc.jar Qualified Name: com.ptc.windchill.org.QueryUtil Java Class Version: 7 (51.0) JD-Core Version: 0.5.3
 */