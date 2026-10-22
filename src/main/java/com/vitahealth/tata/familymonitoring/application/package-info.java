/**
 * Orchestrates scoped follow-up commands, queries and synchronous source projections. Human reads
 * select current consent before returning immutable evidence; source writes share the originating
 * transaction. Private adult reminders use a separate owner-scoped append and query boundary.
 */
package com.vitahealth.tata.familymonitoring.application;
