#ifndef DLIST_H
#define DLIST_H

#include <stddef.h>

typedef struct DListNode {
  void *data;
  struct DListNode *prev;
  struct DListNode *next;
} DListNode;

typedef void (*dlist_free_fn)(void *data);
typedef int (*dlist_cmp_fn)(const void *lhs, const void *rhs);
typedef void (*dlist_iter_fn)(void *data, void *ctx);

typedef struct {
  DListNode *head;
  DListNode *tail;
  size_t size;
  dlist_free_fn free_fn;
} DList;

DList *dlist_create(dlist_free_fn free_fn);
void dlist_clear(DList *list);
void dlist_destroy(DList *list);

size_t dlist_size(const DList *list);
int dlist_is_empty(const DList *list);

DListNode *dlist_push_front(DList *list, void *data);
DListNode *dlist_push_back(DList *list, void *data);
void *dlist_pop_front(DList *list);
void *dlist_pop_back(DList *list);

DListNode *dlist_insert_after(DList *list, DListNode *node, void *data);
DListNode *dlist_insert_before(DList *list, DListNode *node, void *data);
void *dlist_remove_node(DList *list, DListNode *node);

DListNode *dlist_find(const DList *list, const void *key, dlist_cmp_fn cmp_fn);
int dlist_contains(const DList *list, const void *key, dlist_cmp_fn cmp_fn);
void dlist_foreach(DList *list, dlist_iter_fn iter_fn, void *ctx);

#endif
