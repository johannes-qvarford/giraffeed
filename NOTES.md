Giraffeed is a webapp to enhance RSS/ATOM feeds.

A client can request a Feed for a certain FeedType, FeedResource and potentially EnhancementOptions.

FeedTypes can, together with a FeedResource and EnhancementOptions, be used to build a FeedEnhancement, which can resolve a FeedUrl, as well as an EnhancementFeedUrl that will enhance the underlying FeedUrl when resolved.
A FeedDownloader can fetch a Feed based on a FeedUrl.
A FeedType can enhance a Feed.

A FeedSource refers to an underlying FeedUrl.


EnhancementOptions could include OnlyImages, IncludeComments e.g.

There should be a home page where people can enter their feed url and get an enhanced feed url back.