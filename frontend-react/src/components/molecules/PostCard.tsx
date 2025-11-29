import { Link } from "react-router-dom"
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"

interface Post {
    slug: string
    title: string
    content: string
    authorUsername: string
    publishedAt: string
    categoryName: string
    tagNames: string[]
}

interface PostCardProps {
    post: Post
}

export function PostCard({ post }: PostCardProps) {
    return (
        <Card className="h-full flex flex-col">
            <CardHeader>
                <div className="flex justify-between items-start">
                    <CardTitle className="text-xl hover:underline">
                        <Link to={`/posts/${post.slug}`}>{post.title}</Link>
                    </CardTitle>
                    <Badge variant="secondary">{post.categoryName}</Badge>
                </div>
            </CardHeader>
            <CardContent className="flex-grow">
                <p className="text-muted-foreground line-clamp-3">{post.content}</p>
            </CardContent>
            <CardFooter className="flex justify-between text-sm text-muted-foreground">
                <span>By {post.authorUsername}</span>
                <span>{new Date(post.publishedAt).toLocaleDateString()}</span>
            </CardFooter>
        </Card>
    )
}
