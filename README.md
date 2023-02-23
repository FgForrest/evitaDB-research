# evitaDB research project

The main objective of the project is to research and develop a functional software solution for e-commerce. 
Registration number: `CZ.01.1.02/0.0/0.0/19_262/0020308`

The partial aim of the project is to confirm the hypothesis that current traditional technologies are not suitable as 
a search engine for e-commerce solutions and then to develop a new solution that meets today's demanding requirements, 
especially in terms of search speed.

## What's current status of evitaDB?

evitaDB is currently under active development. evitaDB is supported by the company [FG Forrest](https://www.fg.cz),
which specializes in the development of e-commerce stores for large clients in the Czech Republic and abroad. evitaDB
concepts have been proven to work well in production systems with annual sales exceeding 50 million €.

Engineers from FG Forrest cooperate with academic team from [University of Hradec Králové](https://www.uhk.cz), so our
statements about evitaDB performance are backed by thorough (and unbiased) testing and research. All proofs can be found
in [this repository](https://github.com/FgForrest/evitaDB-research), and you can run tests on your HW to verify our conclusions.

The performance tests are implemented using [JMH](https://github.com/openjdk/jmh) and can be run 
using `io.evitadb.BenchmarkRunner` class. The tests working with real-world datasets expect data in folder named `data`
in the root of the Git repository where [this archive is unzipped](https://drive.google.com/file/d/1M4w4ZW2CI2T_IJ7LpNsr4n7xgJkFoV73/view?usp=share_link).

The [instructions for running tests](docs/test_suite.md) are in separate document.

## What's the license of the evitaDB

evitaDB is licensed under the [Business Source License 1.1](LICENSE). Technically, it is not
an open source license, but is an [open source friendly](https://itsfoss.com/making-the-business-source-license-open-source-compliant/)
license, because it automatically converts to one after a period of time specified in the license.

We're fans of open source, and we've benefited a lot from open source software (even the database engine uses some of it).
The database implementation has taken thousands of man-days and, if successful, will take several thousand more. We were
lucky to get an [EU grant](https://evitadb.io/project-info) that partially funded the initial implementation, but we
need to build a self-sustaining project in the long run. [Our company](https://www.fg.cz) uses evitaDB for its own
commercial projects, so the development of the database is guaranteed, but without additional income the development
would be limited. That's why we have chosen this type of license, but in the end we allow you - our users - almost any
use.

**In a nutshell:**

- the BSL license covers a period of 4 years from the date of the software release
- 4 year old version of evitaDB becomes [permissive Apache License, v.2](https://fossa.com/blog/open-source-licenses-101-apache-license-2-0/)
- both BSL and Apache licenses allow you to use evitaDB for OSS and/or commercial projects free of charge
- there is one exception - you may not offer and sell evitaDB as a service to third parties

That's it.

[Read license FAQ](https://evitadb.io/documentation/use/license)

## Research procedure

An API covering the [required functionalities](#query-requirements) will be implemented on top of two general-purpose,
freely available databases and compared with single "greenfield" solution.

All competing solutions will be populated using a data pump with real datasets (both from B2C and B2B environment)
of existing FG Forrest clients, who gave their explicit consent to do so. The client data sets will not contain any
personal data - it will represent only a "sale catalog".

## Evaluation methodology

A single automated test suite will be implemented that will test all the basic scenarios from a real-life e-commerce
solution. This suit will then be run against individual implementations in a laboratory environment and the results
will be recorded.

The criteria for evaluating the best option are:

- cumulative response speed in the test scenarios (weight 75%)
- memory space consumption (weight 20%)
- disk space consumption (weight 5%)

The cumulative response time to (at least) all subsequent search queries is measured:

* category tree display (open to the current category) - menu rendering
* category detail display (retrieving one full category entity) + product listing
* product listing filterable by
    * parameters
    * brands
    * tags
    * prices
* product ordering
    * by selected attribute
    * by price

The cache utilization is problematic in filtering scenario, because there are way too many combination the user can
select. Moreover, the data are frequently changed due and the impact of these changes is hard to translate to cache
record invalidation orders, because of the complex relations between them. The implementations can't thus rely
on the caching layer when the tests are run and the performance is evaluated.

The final evaluation of the reseach is [available here](https://evitadb.io/research/evaluation/evaluation).

## Expected record counts for performance tests

### Entities

Our performance tests will run o datasets that will be similar to those:

| Entity                                                              | Senesi.cz | Signal-nabytek.cz | Fjallraven CZ | Rako CZ | Kili CZ |
|---------------------------------------------------------------------|-----------|-------------------|---------------|---------|---------|
| <Term name="entity: adjustedPricePolicy">adjustedPricePolicy</Term> | 2         | 2                 | 3             | 1       | 110     |
| <Term name="entity: brand">brand</Term>                             | 158       | 42                | 0             | 0       | 76      |
| <Term name="entity: category">category</Term>                       | 202       | 220               | 87            | 16      | 325     |
| <Term name="entity: group">group</Term>                             | 1190      | 20                | 7             | 1       | 265     |
| <Term name="entity: parameterItem">parameterItem</Term>             | 19299     | 3477              | 1346          | 2044    | 2848    |
| <Term name="entity: parameterType">parameterType</Term>             | 255       | 558               | 32            | 48      | 39      |
| <Term name="entity: paymentMethod">paymentMethod</Term>             | 15        | 4                 | 3             | 1       | 5       |
| <Term name="entity: priceList">priceList</Term>                     | 2         | 3                 | 6             | 4       | 7044    |
| <Term name="entity: product">product</Term>                         | 64628     | 50852             | 31144         | 3587    | 26567   |
| <Term name="entity: shippingMethod">shippingMethod</Term>           | 3         | 15                | 6             | 1       | 52      |

<UsedTerms>
    <h4>Legend</h4>

	<dl>
		<dt>entity: adjustedPricePolicy</dt>
		<dd>price policy (discounts, special programs and so on)</dd>
		<dt>entity: brand</dt>
		<dd>brand (such as: Nokia, Samsung and so on)</dd>
		<dt>entity: category</dt>
		<dd>product category (such as: TV, Notebook and so on)</dd>
		<dt>entity: group</dt>
		<dd>product groups (such as: action ware, new items on stock and so on)</dd>
		<dt>entity: parameterType</dt>
		<dd>parameter facet group detail data (such as: color, size, resolution)</dd>
		<dt>entity: parameterItem</dt>
		<dd>parameter facet detail data (such as: blue, yellow, XXL, fullHD)</dd>
		<dt>entity: paymentMethod</dt>
		<dd>form of paying on the site (such as: by credit card, direct transfer and so on)</dd>
		<dt>entity: priceList</dt>
		<dd>entity aggregating prices of product sharing common trait (such as: VIP, sellout)</dd>
		<dt>entity: product</dt>
		<dd>entity that is being sold on the site</dd>
		<dt>entity: shippingMethod</dt>
		<dd>form of delivery of the goods (such as: DPD, PPL, Postal service and so on)</dd>
	</dl>
</UsedTerms>

The final datasets were reduced only to Senesi.cz, Signal-nabytek.cz and new dataset KeramikaSoukup.cz was added.
The record cardinalities in some of those datasets were enlarged considerably (see [evaluation results here](https://evitadb.io/research/evaluation/evaluation#data)).

### Connected data cardinalities:

| Type of data        | Senesi.cz | Signal-nabytek.cz | Fjallraven CZ | Rako CZ | Kili CZ |
|---------------------|-----------|-------------------|---------------|---------|---------|
| price               | 68522     | 59018             | 193680        | 15885   | 1594502 |
| associated data     | 479246    | 326205            | 209694        | 23535   | 298963  |
| localized texts     | 258798    | 353597            | 88913         | 94803   | 102855  |
| attributes          | 1351227   | 924281            | 574488        | 66820   | 572674  |
| facets              | 876080    | 802583            | 334008        | 144161  | 466160  |

Localized texts are part of associated data, they are counted separately, to get grip how big part
they play in associated data set. Localized texts are not counted in the associated data row.

The final datasets were reduced only to Senesi.cz, Signal-nabytek.cz and new dataset KeramikaSoukup.cz was added.
The record cardinalities in some of those datasets were enlarged considerably (see [evaluation results here](https://evitadb.io/research/evaluation/evaluation#data)).

## Validation set

In the initial phase the university teams will select one general-purpose relational database and one NoSQL database.
The criterion for selecting a database machine is:

* the license to run the e-commerce platform must be free of charge
* the database resource must have good documentation
* the database is expected to be further developed or supported in the next 5 to 10 years
* it can run on Linux OS (ideally Ubuntu distribution)

Recommended technologies to start investigation are (the selected set is not complete and can be extended by another
database engine):

* relational database candidates:
    * [MySQL](https://www.mysql.com/) ([MariaDB](https://mariadb.com/), [Percona](https://www.percona.com/))
    * [PostgreSQL](https://www.postgresql.org/)

The selection process is described [in appendix A](https://evitadb.io/research/sql/thesis#appendix-a-postgresql-selection-rationale)
of the associated thesis. The **PostgreSQL** was selected as the platform for prototype implementation on top of
relational database.

* NoSQL database candidates:
    * [MongoDB](https://www.mongodb.com/)
    * [Elasticsearch](https://www.elastic.co/) ([Lucene](https://lucene.apache.org/))

The selection process is described [in appendix A](https://evitadb.io/research/nosql/thesis#appendix-a-elasticsearch-selection-rationale)
of the associated thesis. The **Elasticsearch** was selected as the platform for prototype implementation on top of
relational database.

### Custom "greenfield" solution

The solution will be designed as an in-memory NoSQL database optimized to run on a single machine. The cluster
mode will be designed as single writer, multiple reader replicas with the same dataset, which will be updated using
an event stream from the primary source of truth.

The solution will be based on the assumption that the entire search index can be stored in RAM. Information that is
not used for retrieval but only for display to the user may or may not be stored in memory (we consider the use of
a [memory mapped file](https://medium.com/i0exception/memory-mapped-files-5e083e653b1)).

The data will be stored in memory as a sorted array of numeric primary identifiers of entities (products, categories,
tags, etc.) over which Boolean AND, OR, NOT operations will be performed, and then GROUP BY and SUM operations in
the pricing area. All operations will be performed in a so-called "lazy" manner, which promises better
performance in moments when complete results do not need to be computed (e.g. for the purpose of displaying the category
tree). Optimized data structures such as [inverted index](https://en.wikipedia.org/wiki/Inverted_index) or
[interval tree](https://en.wikipedia.org/wiki/Interval_tree) or [range tree](https://en.wikipedia.org/wiki/Range_tree)
will be used for specific search operations.

The API and implementation will be designed so that as many related operations as possible are computed within a single
request using common intermediate results. The client will not have to compose the functionality with a large number
of database engine calls (in current solutions it is common, that dozens of calls to a generic database engine are
needed to display category details with product listing).

## Developing solutions for practical applicability

Based on the measurements, the best implementation option is selected and refined to production quality, which requires
extensive and clear documentation, coverage of automated unit, integration and performance tests.

The goal is:

* to prepare HTTP APIs for communication with the outside world using commonly used formats:
    * [GraphQL](https://graphql.org/)
    * [REST](https://en.wikipedia.org/wiki/Representational_state_transfer)
    * [gRPC](https://grpc.io/)
* containerization of the distribution package (using [Docker](https://www.docker.com/))
* extending the implementation to a clustered solution (required on all major e-commerce sites)
* publishing source files on generally accepted hosting platforms ([GitHub](https://github.com/),
  [Gitlab](https://about.gitlab.com/) or [BitBucket](https://bitbucket.org/))
* API documentation
* implementation of a sample e-commerce solution on top of this API with a basic dataset in JavaScript /
  [Node.JS](https://nodejs.org/en/)

## Data model

See more detailed [schema API](https://evitadb.io/research/assignment/updating/schema_api) describing data model manipulation.

Minimal entity definition consists of: [Entity type](#entity-type) and [Primary key](#primary-key) (even this is optional
and may be automatically generated by the database). Other entity data is purely optional and may not be used at all.

This combination is covered by interface `evita_api/src/main/java/io/evitadb/api/data/EntityReferenceContract.java`.
Full entity with data, references, attributes and associated data is represented by interface
`evita_api/src/main/java/io/evitadb/api/data/EntityContract.java`.

Schema for entity is described by:
`evita_api/src/main/java/io/evitadb/api/schema/EntitySchema.java`

### Entity type

[String](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/String.html) type of entity.
Entity type is main business key (equivalent to a table name in relational database) - all data of entities with same
type are stored in separated index. Within the entity type the entity is uniquely represented by
[primary key](#primary-key).

Entity is described by its schema:
`evita_api/src/main/java/io/evitadb/api/schema/EntitySchema.java`

Although evitaDB requires schema for each entity type, it supports automatic evolution when you allow it. If you don't
specify otherwise, evitaDB learns about entity attributes, their data types and all necessary relations along the way
when you insert new data to it. Once the attribute, associated data or other contours of the entity is known, they are
enforced by the evitaDB, however. This mechanism somehow resembles the schema-less approach, but results in much more
consistent data store.

The details about [schema definition](https://evitadb.io/research/assignment/updating/schema_api) are part of different document.

### Primary key

Unique [int](https://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html) positive number
(max. 2<sup>63</sup>-1) representing the entity. Can be used for fast lookup for entity (entities). Primary key must be
unique within the same [entity type](#entity-type).

May be left empty if it should be automatically generated by the database.
The primary key allows evitaDB to decide whether the entity should be inserted as new or existing entity should be
updated instead.

### Hierarchical placement

Entities may be organized in hierarchical fashion. That means that entity may refer to single parent entity and may be
referred by multiple child entities. Hierarchy is always composed of entities of same type.

Each entity must be part of at most single hierarchy (tree).

Hierarchy placement is represented by the interface:
`evita_api/src/main/java/io/evitadb/api/data/HierarchicalPlacementContract.java`.

Most of the e-commerce system organize their products in hierarchical category system. The categories are
source for the catalog menus and when user examines the category content he/she usually sees products in entire
category subtree of the category. That's why hierarchies are directly supported by evitaDB.

### Attributes (unique, filterable, sortable, localized)

The entity attributes allow defining set of data that are fetched in bulk along with the entity body.
The attribute may be marked as filterable to enable filtering by it, or sortable to be sorted by.
The attributes are not automatically searchable / sortable in order not to waste precious memory space and save
computational overhead for maintaining and an index for the data that will never be used in queries.

Attributes must be used for all data you want to filter or sort by. Attributes are recommended to be used also for
frequently used data that are associated with the entity (for example "name". "perex", "main motive") even if you don't
necessarily need it for querying purposes.

The attribute provider ([entity](#entity-type) or [reference](#references)) is represented by the interface:
`evita_api/src/main/java/io/evitadb/api/data/AttributesContract.java`

The attribute schema is described by:
`evita_api/src/main/java/io/evitadb/api/schema/AttributeSchema.java`

#### Allowed decimal places

The allowed decimal places setting represent optimization that allows converting rich numeric types (such as
[BigDecimal](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/math/BigDecimal.html) used for
precise number representation) to primitive [int](https://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html)
type that is much more compact and can be used for fast binary search in array/bitset representation. The original rich
format is still present in an attribute container, but internally database uses the primitive form when an attribute is
part of filtering or sorting constraints.

When number cannot be converted to a compact form (for example it has more digits in the fractional part than expected),
exception is thrown and entity update is rejected.

#### Localized attributes

An attribute may contain localized values. It means that different values should be used for filtering / sorting and
should be returned along with entity when certain locale is used in the [search query](https://evitadb.io/research/assignment/querying/query_language).
Localized attributes are standard part of most of the e-commerce systems, and that's why evitaDB provides special
treatment for those.

#### Data types in attributes

Attributes allow using [variety of data types](https://evitadb.io/research/assignment/model/data_types) and their arrays. Database support all basic types,
date-time types and `evita_common/src/main/java/io/evitadb/dataType/Range.java` types.
Range values are allow using special type of [search query](https://evitadb.io/research/assignment/querying/query_language) filtering constraint -
[InRange](https://evitadb.io/research/assignment/querying/query_language#in-range).

This filtering constraint allows filtering entities that are inside the range bounds. For more information see
[InRange](https://evitadb.io/research/assignment/querying/query_language#in-range) documentation.

Any of the supported data type might be wrapped into an array - that means that attribute might represent multiple
values at once. Such attribute cannot be used for sorting, but can be used for filtering, where it fulfills
the filtering constraint when **any** of the values in array match the constraint predicate. This is particularly useful
for ranges where you can for example simply define multiple periods of validity and the [InRange](https://evitadb.io/research/assignment/https://evitadb.io/research/assignment/querying/query_language#in-range)
constraint will match all the entities having at least one period enveloping the input date and time (this is another
frequently present use-case in e-commerce systems).

See chapter [about all supported data types](https://evitadb.io/research/assignment/model/data_types) for more information.

### Associated data

Associated data carry additional data entries that are never used for filtering / sorting but may be needed to be fetched
along with entity in order to display data to the target consumer (i.e. an user / API / bot). Associated data allow
storing all basic [data types](https://evitadb.io/research/assignment/model/data_types) and also complex, document like types.

The complex data type is used for rich objects, such as Java POJOs and [automatically converted by](https://evitadb.io/research/assignment/model/associated_data_implicit_conversion)
to an internal representation that is composed solely of supported data types (or another complex objects) and can be
deserialized back to the client custom POJO on demand providing the POJO structure matches the original document format.

AssociatedData provider ([entity](#entity-type)) is represented by the interface:
`evita_api/src/main/java/io/evitadb/api/data/AssociatedDataContract.java`

Associated data schema is described by:
`evita_api/src/main/java/io/evitadb/api/schema/AssociatedDataSchema.java`

The [search query](https://evitadb.io/research/assignment/querying/query_language) must contain specific [requirement](https://evitadb.io/research/assignment/querying/query_language#require)
to fetch the associated data along with the entity. Associated data are stored and fetched separately by their name.

#### Localized associated data

Associated data value may contain localized values. It means that different values should be returned along with entity
when certain locale is used in the [search query](https://evitadb.io/research/assignment/querying/query_language). Localized data are standard part of most
of the e-commerce systems, and that's why evitaDB provides special treatment for those.

### References

The references, as the name suggest, refer to other entities (of the same or different entity type).
The references allow entity filtering by the attributes defined on the reference relation or the attributes of
the referenced entities. The references enable [statistics](https://evitadb.io/research/assignment/index#parameters-faceted-search) computation if
facet index is enabled for this referenced entity type. The reference is uniquely represented by
[int](https://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html)
positive number (max. 2<sup>63</sup>-1) and [String](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/String.html)
entity type and can represent <Term>facet</Term> that is part of one or multiple <Term name="facet group">facet groups</Term>,
which are also identified by [int](https://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html).
The reference identifier in one entity is unique and belongs to single group id. Among multiple entities the reference
to same referenced entity may be part of different groups.

The referenced entity type may relate to another entity managed by evitaDB, or it may refer to any external entity
possessing unique [int](https://docs.oracle.com/javase/tutorial/java/nutsandbolts/datatypes.html) key as its identifier.
We expect that evitaDB will maintain data only partially, and that it will co-exist with other systems in one runtime -
such as content management systems, warehouse systems, ERPs and so on.

The references may carry additional key-value data linked to this entity relation (fe. item count present on the
relation to a stock). The data on references is subject to the same rules as [entity attributes](#attributes-unique-filterable-sortable-localized).

Reference is represented by the interface: `evita_api/src/main/java/io/evitadb/api/data/ReferenceContract.java`.
Reference schema is described by: `evita_api/src/main/java/io/evitadb/api/schema/ReferenceSchema.java`

### Prices

Prices are specific to a very few entity types (usually products, shipping methods and so on), but because correct price
computation is very complex and important part of the e-commerce systems and highly affects performance of the entities
filtering and sorting, they deserve first class support in entity model. It is pretty common in B2B systems a single
product has assigned dozens of prices for the different customers.

Price provider is represented by the interface:
`evita_api/src/main/java/io/evitadb/api/data/PricesContract.java`
Single price is represented by the interface:
`evita_api/src/main/java/io/evitadb/api/data/PriceContract.java`

Price schema is part of main entity schema:
`evita_api/src/main/java/io/evitadb/api/schema/EntitySchema.java`

For detail information about price for sale computation [see this article](https://evitadb.io/research/assignment/querying/price_computation).

## Entity indexing

See more detailed [entity API](https://evitadb.io/research/assignment/updating/entity_api) describing entity manipulation.

The entity indexing is a mechanism that stores [entity data](#data-model) into the persistent storage and prepares
the data for searching. We distinguish two types of this mechanism:

- [bulk](#bulk-indexation)
- [incremental](#incremental-indexation)

### Bulk indexing

Bulk indexing is used for rapid indexing of large quantities of source data. It's used for initial catalog setup from
external (primary) data store. It doesn't need to support transactions. Whenever something goes wrong, the work in
progress might be thrown away entirely without affecting any clients (because it's initial DB setup, no client is
reading from it yet). The goal here is to index hundreds or thousands entities per second.

Bulk indexation is executed in single-threaded fashion.

### Incremental indexing

Incremental indexing is used for keeping index up-to-date during its lifetime. We expect some form of [change data
capture](https://en.wikipedia.org/wiki/Change_data_capture) process is incorporated in the primary data store.
Incremental indexing must support transactions at least with the [read commited isolation level](https://en.wikipedia.org/wiki/Isolation_(database_systems)#Read_committed).
Initial implementation may relax the concurrency and limit parallelism so that only single write transaction may be open
at a time. Multiple parallel read transactions must be supported in the final implementation since the beginning and
simultaneous read/write transactions must be supported as well.

Rolled back transaction must not affect the working data set. Committed transaction must leave data set in consistent
state and must be resistant to unexpected process termination or hardware failure (committed data should enforce fsync
to persistent disk storage).

Incremental indexing should be as fast as possible, but since there are bigger requirements it is expected to be
considerably slower than [bulk indexation](#bulk-indexing).

## Data fetching

Only primary keys of the entities are returned to the query result by default. Each entity in this simplest case is
represented by `evita_api/src/main/java/io/evitadb/api/data/EntityReferenceContract.java`
interface.

Client application can request returning entity bodies instead, but this must be explicitly requested by using specific
require constraint:

- [entity fetch](https://evitadb.io/research/assignment/querying/query_language#entity-body)
- [attribute fetch](https://evitadb.io/research/assignment/querying/query_language#attributes)
- [associated data fetch](https://evitadb.io/research/assignment/querying/query_language#associated-data)
- [price fetch](https://evitadb.io/research/assignment/querying/query_language#prices)

When such a require constraint is used, data are fetched *greedily* during initial query. Response object will then
contain entities in the form of `evita_api/src/main/java/io/evitadb/api/data/EntityContract.java`.

### Lazy fetching (enrichment)

Attributes, associated data and prices can be fetched separately by providing primary key of the entity. Initial entity
loaded by [entity fetch](https://evitadb.io/research/assignment/querying/query_language#entity-body) or by limited set of requirements can be lazily expanded (enriched)
with additional data by so-called *lazy loading*.

This process loads above-mentioned data separately and adds them to the entity object anytime after it was initially
fetched from evitaDB. Due to immutability characteristics enforced by database design, the entity object enrichment
leads to a new instance.

Lazy fetching may not be necessary for frontend designed using MVC architecture, where all requirements for the page
are known prior to rendering. But different architectures might fetch thinner entity forms and later discover that
they need more data in it. While this approach is not optimal performance-wise, it might make the life for developers
easier, and it's much more optimal to just enrich existing query (using lookup by primary key and fetching only missing
data) instead of re-fetching entire entity again.

## Query requirements

See more detailed [query API](https://evitadb.io/research/assignment/querying/query_api) and [query language](https://evitadb.io/research/assignment/querying/query_language) in
separate chapters.

The querying is the hearth of each database, and therefore the core of the query language was designed upfront
the prototype implementation phase along with the unified functional test suite. When the first versions of
the prototype implementations were created, the functional suite accompanied also performance test suite, first for
artificial data set and later for real customer data sets.

This chapter briefly describes the use-cases the e-commerce catalog frequently solve and which we try to cover in
our [query language](https://evitadb.io/research/assignment/querying/query_language) and the [API](https://evitadb.io/research/assignment/querying/query_api).

#### Attributes

We need to fully support basic [boolean algebra](https://en.wikipedia.org/wiki/Boolean_algebra) (AND, OR, NOT) with
[parentheses grouping logic](https://en.wikipedia.org/wiki/Bracket#Parentheses_in_programming_languages) that supports
predicates:

* **numeric:** equals, greater than, lesser than, between, in range
* **temporal:** equals, greater than, lesser than, between, in range
* **string:** contains, starts with, ends with
* **boolean:** is null, is not null

##### Localized search

The items in the multi-language catalogs often have only limited set of localizations (translations). The search
engine must easily filter only those items that are available in selected locale. Let's assume that specific
<Term>brand</Term> is referred by items with following localizations:

* **Product A**: EN, CZ, IT
* **Product B**: EN, DE, FR
* **Product C**: EN, CZ, PL

When the query lists all items referring to that brand and specifies the locale equal to `EN`, all items needs to be
returned, while using locale equal to `CZ` lists only items `Product A` and `Product B`.

#### Parameters (faceted search)

Faceted search is commonly used on all major e-commerce sites. Facets represent properties of items in certain area that
are significant for customers to pick correct item to buy. Faceted search could be usually found on <Term>category</Term>
drill down view, specific <Term>group</Term> pages or <Term>fulltext search</Term> result page.

According to some studies - properly implemented faceted search (sometimes called as parametrized search) can [increase
conversions of the e-commerce sites by 20%](https://searchspring.com/how-experts-optimize-category-navigation-in-ecommerce/).
Each product can have multiple parameters in the form of key-value map. Values might represent:

- discrete constants (for example color:black, size:XXL, OS:Android) visualized as checkboxes or selects
- or numeric values that are spread out in some range and can be visualized as a slider with min and max boundaries

Parameters - <Term name="facet">facets</Term> are organized and grouped to <Term name="facet group">facet groups</Term>
by their similarity (color, size, operation system). When user enters the category page (or other item listing view)
he/she should see only facets (facet groups) that have sense in that category. In other words - the only facets visible
in the filter are those that are linked to (referenced by) at least one item visible in the view (ignoring pagination
settings).

For better customer orientation how certain facet narrows the item listing e-commerce sites display
the number of the items that have certain property next to it - see example (numbers in brackets):

[![Faceted search on Alzashop.com](assets/faceted_search_example.png "Faceted search on Alzashop.com")](https://www.alzashop.com/processors/18842843.htm)

Even better approach is to reflect the currently selected filter in the faceted filter itself. Additional facets
that would return no result if selected are displayed as disabled (see grayed properties with zero brackets in above
example).

See [description of the facet summary object](https://evitadb.io/research/assignment/classes/facet_lookup_summary) for better understanding.

##### Interval properties

E-commerce filters contain not only facets but also sliders that allow user to search for items having attribute
in certain value range. For example when you buy refrigerator you are usually constrained by the space at your disposal,
and you need to set limits for width, depth and height of the refrigerator.

Example:

[![Faceted search on Senesi.cz](assets/faceted_search_interval.png "Faceted search on Senesi.cz")](https://www.senesi.cz/umyvadla)

**Search engine must:**

* return thresholds for each interval property (stored as entity [attribute](#attributes-unique-filterable-sortable-localized)):
    * highest value of the property in the item view
    * lowest value of the property in the item view
* optionally: compute histogram of describing attribute values computed from the items in this view (i.e. threshold with
  count of items having this property in respective histogram interval)

See [histogram object](https://evitadb.io/research/assignment/classes/histogram) for better understanding.

##### Inverted relations

Facets in the same facet group are usually combined by boolean OR (disjunction) relation, facets in different groups are
combined by boolean AND (conjunction) relation. These relations might be inverted in some edge cases and the database
must support definition of inverted relations among facet groups and the facets within certain group.

##### Negative properties

Some facets might have negative meaning - so that if user marks them, he/she expects that listing will contain only
items that don’t have such property (as an example consider facet allergen:gluten which will cause that all items
containing gluten will be removed from the listing).

**When facet with negative meaning (defined on property group) is selected in the filter, search engine must:**

* all items having such property will be EXCLUDED from the result
* predicted numbers of selected records are properly computed upfront

##### Impact statistics

Facets that would further expand count of the matching items (if selected) display difference count with the plus sign,
or the updated overall result count next to them. See example below (numbers in brackets):

[![Extended facet statistics on CZC.cz](assets/faceted_search_extended_statistics.png "Extended facet statistics on CZC.cz")](https://www.czc.cz/herni-notebooky/produkty?q-c-0-f_137581190=sSSD)

**When any facet filtering is applied in passed database query, search engine must:**

* return extended statistics computed for all other facets that contains information about:
    * how many items are added to result if facet would be added to filtering
    * how many items are removed from result if facet would be added to filtering
    * how many items remain in the result if facet would be added to filtering
* correctly apply OR / AND / NOT relations defined by property groups

#### Prices

The search engine can search for products whose *price for sale* falls within the price range specified by the user.
The price for sale must respect selected currency, price date/time validity range and relates to one of the price list
identifiers available to the user. All this information is part of the input query.

For master products, the lowest price of any product variant is used. For complete sets, dual behaviour must be
supported:

- if a price is set directly for a set, count with this price
- if not, the most preferred price for each item of such a set must be added up and the resulting amount calculated

Exact and detailed [price for sale computation algorithm](https://evitadb.io/research/assignment/querying/price_computation) is described in separate chapter.

##### Price histogram

Optionally, we would like to be able to generate a histogram of prices similar to
[interval properties](#interval-properties).

#### Brands / groups

E-commerce sites have special landing pages for <Term name="brand">brands</Term> / manufacturers or <Term name="group">groups</Term>.
These landing pages behave similar to category detail page and display all items directly related to the brand / group.
The search engine must be able to compute facets and all other information for all entities that relate (have reference to)
specified external entity.

##### The number of products of a given brand in the categories

The search engine needs to look up for all hierarchy placements (categories) where at least one product of that brand
is (directly or transitively) located for any of the <Term>brand</Term> or <Term>group</Term>. The detail of the brand
frequently lists all categories of products the brand produces.

#### Tags

Tags behave in the same way as described in the [faceted search chapter](#parameters-faceted-search). The search engine
must be able to return a list of labels that are used in any currently visible product in the selected category and
subcategories.

#### Fulltext search

The solution should allow to easily combine full-text search with parametrized (faceted search). The fulltext search
may not be implemented initially, but should offer a mechanism for integration external fulltext system.

##### Hierarchical search and tree exclusion

<Term name="category">Categories</Term> are usually organized in hierarchical fashion. Single product may be listed
in one or more categories. E-commerce sites usually display all products in specific category or its subcategories.
Certain categories might be excluded from the displaying by the site owner or might be accessible only to a subset
of the frontend users (by design). The search engine should take all these requirements into an account and should
transparently exclude all items that are part only of the excluded category subtree.

The search engine should assist in menu generation from hierarchical tree in realtime, listing only those hierarchical
entities (<Term name="category">categories</Term>) that:

* match basic attribute predicate: for example are marked as visible, are valid for display at certain moment in time etc.
* contain at least one visible product in any child level, the product must:
    * match independent attribute query
    * match requested locale
    * produce price for sale

For each of returned entity (<Term>category</Term>) the search engine must:

* compute the overall count of all items available at this entity (<Term>category</Term>)
    * items that have an invisible parent (<Term>category</Term> that doesn't match its own predicate) must not be counted
    * a single item (<Term>product</Term>) may relate to more than one hierarchical parent  (<Term>category</Term>),
      the fact that a product is not counted in one category axis must not affect the other visibility axis
    * counts of lower level nodes are automatically counted in the overall count of their parent category
* produce result in a tree-like structure friendly for rendering

#### Product sorting

The search engine must be able to sort results by:

* **attribute:** for example number of stars, number of sales
* **multiple attributes:** there are special cases where one attribute contains less selective values and another
  attribute is required for predictable search results
* **price for sale:** cheapest, most expensive

##### Personalized sorting

As part of the research, it would be useful to look at the possibilities of personalized sorting, which would allow
a user to be presented with the results that are likely to be of interest to them first, based on their previous
experience with that user (i.e., based on their previous purchases or visits).

For this purpose, it will probably be necessary to use one of the "shallow" artificial intelligence (machine learning)
algorithms and construct a personalised search index in such a way that the search is not slowed down.

This functionality is not critical to the evaluation of the winning approach - it is an add-on functionality that
no existing database currently includes as part of its functionality. However, we know that global trends are moving
in this direction.