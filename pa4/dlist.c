#include "dlist.h"
#include <stdio.h>
#include <stdlib.h>

static DListNode *dlist_node_create(void *data) {
  DListNode *node = calloc(1, sizeof(DListNode));
  if (!node)
    return NULL;

  node->data = data;
  return node;
}

DList *dlist_create(dlist_free_fn free_fn) {
  DList *list = calloc(1, sizeof(DList));
  if (!list)
    return NULL;

  list->free_fn = free_fn;
  return list;
}

void dlist_clear(DList *list) {
  if (!list)
    return;

  DListNode *curr = list->head;
  while (curr) {
    DListNode *next = curr->next;
    if (list->free_fn)
      list->free_fn(curr->data);
    free(curr);
    curr = next;
  }

  list->head = NULL;
  list->tail = NULL;
  list->size = 0;
}

void dlist_destroy(DList *list) {
  if (!list)
    return;

  dlist_clear(list);
  free(list);
}

size_t dlist_size(const DList *list) {
  return list ? list->size : 0;
}

int dlist_is_empty(const DList *list) {
  return dlist_size(list) == 0;
}

DListNode *dlist_push_front(DList *list, void *data) {
  if (!list)
    return NULL;

  DListNode *node = dlist_node_create(data);
  if (!node)
    return NULL;

  node->next = list->head;
  if (list->head)
    list->head->prev = node;
  else
    list->tail = node;

  list->head = node;
  list->size++;

  return node;
}

DListNode *dlist_push_back(DList *list, void *data) {
  if (!list)
    return NULL;

  DListNode *node = dlist_node_create(data);
  if (!node)
    return NULL;

  node->prev = list->tail;
  if (list->tail)
    list->tail->next = node;
  else
    list->head = node;

  list->tail = node;
  list->size++;

  return node;
}

void *dlist_pop_front(DList *list) {
  if (!list || !list->head)
    return NULL;

  DListNode *node = list->head;
  void *data = node->data;

  list->head = node->next;
  if (list->head)
    list->head->prev = NULL;
  else
    list->tail = NULL;

  free(node);
  list->size--;

  return data;
}

void *dlist_pop_back(DList *list) {
  if (!list || !list->tail)
    return NULL;

  DListNode *node = list->tail;
  void *data = node->data;

  list->tail = node->prev;
  if (list->tail)
    list->tail->next = NULL;
  else
    list->head = NULL;

  free(node);
  list->size--;

  return data;
}

DListNode *dlist_insert_after(DList *list, DListNode *node, void *data) {
  if (!list || !node)
    return NULL;

  if (node == list->tail)
    return dlist_push_back(list, data);

  DListNode *new_node = dlist_node_create(data);
  if (!new_node)
    return NULL;

  new_node->prev = node;
  new_node->next = node->next;
  node->next->prev = new_node;
  node->next = new_node;
  list->size++;

  return new_node;
}

DListNode *dlist_insert_before(DList *list, DListNode *node, void *data) {
  if (!list || !node)
    return NULL;

  if (node == list->head)
    return dlist_push_front(list, data);

  DListNode *new_node = dlist_node_create(data);
  if (!new_node)
    return NULL;

  new_node->next = node;
  new_node->prev = node->prev;
  node->prev->next = new_node;
  node->prev = new_node;
  list->size++;

  return new_node;
}

void *dlist_remove_node(DList *list, DListNode *node) {
  if (!list || !node)
    return NULL;

  if (node == list->head)
    return dlist_pop_front(list);
  if (node == list->tail)
    return dlist_pop_back(list);

  void *data = node->data;
  node->prev->next = node->next;
  node->next->prev = node->prev;
  free(node);
  list->size--;

  return data;
}

DListNode *dlist_find(const DList *list, const void *key, dlist_cmp_fn cmp_fn) {
  if (!list || !cmp_fn)
    return NULL;

  for (DListNode *node = list->head; node; node = node->next) {
    if (cmp_fn(node->data, key) == 0)
      return node;
  }

  return NULL;
}

int dlist_contains(const DList *list, const void *key, dlist_cmp_fn cmp_fn) {
  return dlist_find(list, key, cmp_fn) != NULL;
}

void dlist_foreach(DList *list, dlist_iter_fn iter_fn, void *ctx) {
  if (!list || !iter_fn)
    return;

  for (DListNode *node = list->head; node; node = node->next)
    iter_fn(node->data, ctx);
}

const char *dlist_tostring(const DList *list) {
  static char buf[160];
  if (!list) {
    snprintf(buf, sizeof(buf), "DList{null}");
    return buf;
  }

  snprintf(buf,
           sizeof(buf),
           "DList{size=%zu, head=%p, tail=%p}",
           list->size,
           (void *)list->head,
           (void *)list->tail);
  return buf;
}
