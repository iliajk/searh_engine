# Search Engine

This project demonstrates my skills and practical experience by creating a search engine as a Spring application. The search engine is a JAR file that can be run on any server or computer. 
It works with a locally installed MySQL database and has a simple web interface and API for managing and retrieving search results.

## Principles of the Search Engine

1. **Configuration File:** Before running the application, site addresses for the search engine to crawl are specified in a configuration file.
2. **Web Crawling and Indexing:** The search engine automatically crawls all pages of the specified sites and indexes them, creating an index to find the most relevant pages for any search query.
3. **API Search Requests:** Users send queries via the engine's API. A query is a set of words to search for on the site.
4. **Word Transformation:** The query is transformed into a list of words in their base form. For example, nouns are converted to their nominative singular form.
5. **Index Search:** The engine searches the index for pages containing all these words.
6. **Ranking and Sorting:** The search results are ranked, sorted, and returned to the user.

## Features
- **
- **Spring Application:** Easily deployable as a JAR file.
- **MySQL Database:** Works with a locally installed MySQL database.
- **Web Interface:** Simple web interface for user interactions.
- **API:** Comprehensive API for managing the search engine and retrieving search results.

## Getting Started

### Prerequisites

- Java 17 or higher
- MySQL database
