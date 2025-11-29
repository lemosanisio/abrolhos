import { useQuery } from "@tanstack/react-query"
import api from "@/lib/api"
import { PostCard } from "@/components/molecules/PostCard"
import { Navbar } from "@/components/organisms/Navbar"

interface Post {
    slug: string
    title: string
    content: string
    authorUsername: string
    publishedAt: string
    categoryName: string
    tagNames: string[]
}

export function HomePage() {
    const { data: posts, isLoading, error } = useQuery<Post[]>({
        queryKey: ["posts"],
        queryFn: async () => {
            const response = await api.get("/posts")
            return response.data
        },
    })

    return (
        <div className="min-h-screen bg-background">
            <Navbar />
            <main className="container mx-auto px-4 py-8">
                <h1 className="text-3xl font-bold mb-8">Latest Posts</h1>
                {isLoading && <p>Loading...</p>}
                {error && <p className="text-red-500">Error loading posts</p>}
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {posts?.map((post) => (
                        <PostCard key={post.slug} post={post} />
                    ))}
                </div>
            </main>
        </div>
    )
}
