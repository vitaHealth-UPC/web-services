/**
 * Coordinates stock commands, immutable query results and durable confirmed-intake recovery. Source
 * publication retains the stock transaction; processing and retry steps own independent
 * transactions.
 */
package com.vitahealth.tata.inventoryreplenishment.application;
