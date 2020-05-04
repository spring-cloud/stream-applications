/*
 * Copyright 2015-2020 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.fn.consumer.cassandra.domain;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

/**
 * Test POJO
 *
 * @author David Webb
 * @author Artem Bilan
 */
@Table("book")
public class Book {

	@PrimaryKey
	private UUID isbn;

	private String title;

	private String author;

	private int pages;

	private LocalDate saleDate;

	private boolean inStock;

	public Book() {
	}

	public Book(UUID isbn, String title, String author) {
		this.isbn = isbn;
		this.title = title;
		this.author = author;
	}

	/**
	 * @return Returns the isbn.
	 */
	public UUID getIsbn() {
		return this.isbn;
	}

	/**
	 * @return Returns the saleDate.
	 */
	public LocalDate getSaleDate() {
		return this.saleDate;
	}

	/**
	 * @param saleDate The saleDate to set.
	 */
	public void setSaleDate(LocalDate saleDate) {
		this.saleDate = saleDate;
	}

	/**
	 * @return Returns the inStock.
	 */
	public boolean isInStock() {
		return this.inStock;
	}

	/**
	 * @param inStock The isInStock to set.
	 */
	public void setInStock(boolean inStock) {
		this.inStock = inStock;
	}

	/**
	 * @param isbn The isbn to set.
	 */
	public void setIsbn(UUID isbn) {
		this.isbn = isbn;
	}

	/**
	 * @return Returns the title.
	 */
	public String getTitle() {
		return this.title;
	}

	/**
	 * @param title The title to set.
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @return Returns the author.
	 */
	public String getAuthor() {
		return this.author;
	}

	/**
	 * @param author The author to set.
	 */
	public void setAuthor(String author) {
		this.author = author;
	}

	/**
	 * @return Returns the pages.
	 */
	public int getPages() {
		return this.pages;
	}

	/**
	 * @param pages The pages to set.
	 */
	public void setPages(int pages) {
		this.pages = pages;
	}

	@Override
	public String toString() {
		return ("isbn -> " + this.isbn) + "\n" + "tile -> " + this.title + "\n" + "author -> " + this.author
				+ "\n" + "pages -> " + this.pages + "\n";
	}

}
