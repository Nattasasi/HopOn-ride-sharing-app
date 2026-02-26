// web/src/app/posts/create/page.tsx
'use client';

import { useState, useEffect } from 'react'; // Import useEffect
import { useRouter } from 'next/navigation';
import axios from 'axios';
import { Input, Button } from '@/components/ui';
import { useAuth } from '@/app/components/AuthContext'; // Adjust path if necessary

export default function CreatePostPage() {
  const router = useRouter();
  const { userId, isLoggedIn } = useAuth(); // Get userId and isLoggedIn from AuthContext
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    start_location_name: '',
    start_lat: '',
    start_lng: '',
    end_location_name: '',
    end_lat: '',
    end_lng: '',
    departure_time: '',
    total_seats: '',
    price_per_seat: '',
  });
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  // Redirect if not logged in
  useEffect(() => {
    if (!isLoggedIn) {
      router.push('/');
    }
  }, [isLoggedIn, router]); // Dependency array to re-run when isLoggedIn or router changes

  if (!isLoggedIn) {
    // Render a placeholder or null while redirecting
    return <div className="p-8 text-center">Please log in to create a post. Redirecting...</div>;
  }

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prevData) => ({
      ...prevData,
      [name]: value,
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      const token = localStorage.getItem('token'); // Retrieve token
      if (!token) {
        setError('Authentication token not found. Please log in again.');
        setLoading(false);
        return;
      }

      // Prepare data, converting numbers
      const postData = {
        ...formData,
        start_lat: parseFloat(formData.start_lat),
        start_lng: parseFloat(formData.start_lng),
        end_lat: parseFloat(formData.end_lat),
        end_lng: parseFloat(formData.end_lng),
        total_seats: parseInt(formData.total_seats),
        price_per_seat: parseFloat(formData.price_per_seat),
      };

      // Set axios default base URL if not already set globally
      // (This should ideally be done once in a global config)
      axios.defaults.baseURL = axios.defaults.baseURL || 'http://localhost:5000';


      await axios.post('/api/v1/posts', postData, {
        headers: {
          Authorization: `Bearer ${token}`, // Attach token for authentication
        },
      });

      alert('Carpool post created successfully!');
      router.push('/posts'); // Redirect to posts list after creation
    } catch (err: any) {
      console.error('Failed to create carpool post:', err);
      if (axios.isAxiosError(err) && err.response && err.response.data && err.response.data.message) {
        setError(err.response.data.message);
      } else if (axios.isAxiosError(err) && err.response && err.response.data && err.response.data.errors) {
          // Handle validation errors from express-validator
          const validationErrors = err.response.data.errors.map((e: any) => e.msg).join(', ');
          setError(`Validation Failed: ${validationErrors}`);
      }
      else {
        setError('An unexpected error occurred while creating the post.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex flex-col items-center justify-center p-8 bg-zinc-50 dark:bg-black min-h-screen">
      <div className="bg-white dark:bg-gray-800 p-8 rounded-lg shadow-lg w-full max-w-2xl">
        <h1 className="text-3xl font-bold mb-6 text-center text-black dark:text-white">Create New Carpool Post</h1>
        {error && <p className="text-red-500 text-center mb-4">{error}</p>}
        <form onSubmit={handleSubmit} className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <Input
            name="title"
            placeholder="Title"
            value={formData.title}
            onChange={handleChange}
            required
            className="col-span-2 border border-gray-300 px-3 py-2 rounded-md text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 w-full"
          />
          <Input
            name="description"
            placeholder="Description"
            value={formData.description}
            onChange={handleChange}
            required
            className="col-span-2 border border-gray-300 px-3 py-2 rounded-md text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 w-full"
          />

          <h2 className="col-span-2 text-xl font-semibold mt-4 text-black dark:text-white">Start Location</h2>
          <Input
            name="start_location_name"
            placeholder="Start Location Name (e.g., 'Home')"
            value={formData.start_location_name}
            onChange={handleChange}
            required
            className="col-span-2 border border-gray-300 px-3 py-2 rounded-md text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 w-full"
          />
          <Input
            type="number"
            name="start_lat"
            placeholder="Start Latitude"
            value={formData.start_lat}
            onChange={handleChange}
            step="any"
            required
            className="border border-gray-300 px-3 py-2 rounded-md text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 w-full"
          />
          <Input
            type="number"
            name="start_lng"
            placeholder="Start Longitude"
            value={formData.start_lng}
            onChange={handleChange}
            step="any"
            required
            className="border border-gray-300 px-3 py-2 rounded-md text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 w-full"
          />

          <h2 className="col-span-2 text-xl font-semibold mt-4 text-black dark:text-white">End Location</h2>
          <Input
            name="end_location_name"
            placeholder="End Location Name (e.g., 'Office')"
            value={formData.end_location_name}
            onChange={handleChange}
            required
            className="col-span-2 border border-gray-300 px-3 py-2 rounded-md text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 w-full"
          />
          <Input
            type="number"
            name="end_lat"
            placeholder="End Latitude"
            value={formData.end_lat}
            onChange={handleChange}
            step="any"
            required
            className="border border-gray-300 px-3 py-2 rounded-md text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 w-full"
          />
          <Input
            type="number"
            name="end_lng"
            placeholder="End Longitude"
            value={formData.end_lng}
            onChange={handleChange}
            step="any"
            required
            className="border border-gray-300 px-3 py-2 rounded-md text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 w-full"
          />

          <h2 className="col-span-2 text-xl font-semibold mt-4 text-black dark:text-white">Ride Details</h2>
          <Input
            type="datetime-local" // For local date-time input
            name="departure_time"
            placeholder="Departure Time"
            value={formData.departure_time}
            onChange={handleChange}
            required
            className="col-span-2 border border-gray-300 px-3 py-2 rounded-md text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 w-full"
          />
          <Input
            type="number"
            name="total_seats"
            placeholder="Total Seats"
            value={formData.total_seats}
            onChange={handleChange}
            min="1"
            required
            className="border border-gray-300 px-3 py-2 rounded-md text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 w-full"
          />
          <Input
            type="number"
            name="price_per_seat"
            placeholder="Price Per Seat"
            value={formData.price_per_seat}
            onChange={handleChange}
            step="0.01"
            min="0"
            required
            className="border border-gray-300 px-3 py-2 rounded-md text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 w-full"
          />

          <Button type="submit" className="col-span-2 bg-blue-600 text-white hover:bg-blue-700 py-3 rounded-md text-lg font-semibold" disabled={loading}>
            {loading ? 'Creating Post...' : 'Create Carpool Post'}
          </Button>
        </form>
      </div>
    </div>
  );
}
