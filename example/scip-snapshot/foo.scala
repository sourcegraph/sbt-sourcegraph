  case class AccountId(x: Int) extends AnyVal
//           ^^^^^^^^^ definition semanticdb maven _empty_/AccountId#
//           documentation ```scala
//           ^^^^^^^^^ definition semanticdb maven _empty_/AccountId.apply().
//           documentation ```scala
//           relationship semanticdb maven scala/Function1#apply(). implementation reference
//           ^^^^^^^^^ definition semanticdb maven _empty_/AccountId#copy().
//           documentation ```scala
//           ^^^^^^^^^ definition semanticdb maven _empty_/AccountId#productElement().
//           documentation ```scala
//           relationship semanticdb maven scala/Product#productElement(). implementation reference
//           ^^^^^^^^^ definition semanticdb maven _empty_/AccountId.
//           documentation ```scala
//           ^^^^^^^^^ definition semanticdb maven _empty_/AccountId#productElementName().
//           documentation ```scala
//           relationship semanticdb maven scala/Product#productElementName(). implementation reference
//                     definition semanticdb maven _empty_/AccountId#<init>().
//                    documentation ```scala
//                     ^ definition semanticdb maven _empty_/AccountId#x.
//                     documentation ```scala
//                     ^ definition semanticdb maven _empty_/AccountId.apply().(x)
//                     documentation ```scala
//                     ^ definition semanticdb maven _empty_/AccountId#copy().(x)
//                     documentation ```scala
//                     ^ definition semanticdb maven _empty_/AccountId#<init>().(x)
//                     documentation ```scala
//                        ^^^ reference semanticdb maven scala/Int#
//                                     ^^^^^^ reference semanticdb maven scala/AnyVal#
  
// reference semanticdb maven scala/AnyVal#<init>().
  object Main {
//       ^^^^ definition semanticdb maven _empty_/Main.
//       documentation ```scala
    def main(): Unit = {
//      ^^^^ definition semanticdb maven _empty_/Main.main().
//      documentation ```scala
//              ^^^^ reference semanticdb maven scala/Unit#
      AccountId(42).x
//    ^^^^^^^^^ reference semanticdb maven _empty_/AccountId.
//                  ^ reference semanticdb maven _empty_/AccountId#x.
      val x = classOf[AccountId]
//        ^ definition local 1
//        documentation ```scala
//            ^^^^^^^ reference semanticdb maven scala/Predef.classOf().
//                    ^^^^^^^^^ reference semanticdb maven _empty_/AccountId#
      val y = List.empty[AccountId]
//        ^ definition local 2
//        documentation ```scala
//            ^^^^ reference semanticdb maven scala/package.List.
//                 ^^^^^ reference semanticdb maven scala/collection/immutable/List.empty().
//                       ^^^^^^^^^ reference semanticdb maven _empty_/AccountId#
    }
  }
  
