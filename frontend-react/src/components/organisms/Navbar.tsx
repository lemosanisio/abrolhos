import { Link } from "react-router-dom"
import { Button } from "@/components/ui/button"
import { useAuth } from "@/context/AuthContext"

export function Navbar() {
    const { isAuthenticated, logout } = useAuth()

    return (
        <nav className="border-b">
            <div className="container mx-auto flex h-16 items-center justify-between px-4">
                <Link to="/" className="text-2xl font-bold">
                    Abrolhos
                </Link>
                <div className="flex items-center gap-4">
                    <Link to="/" className="text-sm font-medium transition-colors hover:text-primary">
                        Home
                    </Link>
                    {isAuthenticated ? (
                        <>
                            <Link to="/admin" className="text-sm font-medium transition-colors hover:text-primary">
                                Dashboard
                            </Link>
                            <Button variant="ghost" onClick={logout}>
                                Logout
                            </Button>
                        </>
                    ) : (
                        <Link to="/login">
                            <Button variant="outline">Login</Button>
                        </Link>
                    )}
                </div>
            </div>
        </nav>
    )
}
